package slicing

import java.io.File
import java.net.URL
import java.nio.file.Files
import util.control.Breaks._

import analysis.config.AnalysisConfig
import evaluation.ResultAnalyzer
import extensions.ExtensionMethods._
import org.opalj.ai.PC
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.{GETSTATIC, Instruction, LoadClass, MethodInvocationInstruction, NEW, NEWARRAY, PUTSTATIC}

import scala.collection.JavaConverters._
import scala.collection.{LinearSeq, mutable}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/*
  This object builds the new class files by inserting the sliced methods.
  It also finds additional dead methods based on the results of the slicing and incorporates configured classes from the config file
 */
object ClassSlicer {


  def sliceClasses(project: Project[URL], analysisConfig: AnalysisConfig, deadInstructionsPerMethodExt: mutable.Map[Method, Iterable[(PC, Instruction)]], callGraph: CallGraph): ClassModificationResult = {
    val deadMethods = findDeadMethods(project, project.allLibraryClassFiles.toList, deadInstructionsPerMethodExt.toMap, callGraph)

    //remove all dead methods
    deadMethods.filter(m => deadInstructionsPerMethodExt.contains(m)).map(deadInstructionsPerMethodExt.remove)
    val slicingResults = deadInstructionsPerMethodExt.par.map(tuple => MethodSlicer.sliceMethod(tuple._1, tuple._2, analysisConfig.debug)).seq
    val slicedMethods = slicingResults.collect {
      case SlicingSuccess(originalMethod, methodTemplate, _) => (methodTemplate, originalMethod.classFile)
      case SlicingFailure(originalMethod, _, _) => (originalMethod.copy(), originalMethod.classFile)
    }.toList

    val methodsByClassFile: mutable.Map[ClassFile, Iterable[MethodTemplate]] =
      scala.collection.mutable.Map(slicedMethods.groupBy(t => t._2).mapValues(l => l.map(t => t._1)).toSeq: _*)

    //inject class files from configuration
    val injectedClasses = getClassesFromConfigFile(project, analysisConfig.configPath).toListBuffer
    val loadedClasses = getConstantLoadedClasses(project, methodsByClassFile.keys).map(c => (c, c.methods))
    injectedClasses ++= loadedClasses
    for (configuredClass <- injectedClasses) {
      val methods = configuredClass._2.map(m => m.copy()).toList
      if (methodsByClassFile.contains(configuredClass._1)) {
        val currentMethods = methodsByClassFile(configuredClass._1).toListBuffer
        val newMethods = methods.filterNot(m => currentMethods.exists(m2 => m2.name == m.name && m.descriptor.equals(m2.descriptor)))
        val intersect = currentMethods.map(m => (m, methods.find(m2 => m2.name == m.name && m.descriptor.equals(m2.descriptor))))
        intersect.foreach(m => {
          m._2 match {
            case Some(otherMethod) =>
              currentMethods.update(currentMethods.indexOf(m._1), otherMethod)
            case _ =>
          }
        })
        currentMethods ++= newMethods
        methodsByClassFile(configuredClass._1) = currentMethods
      } else {
        methodsByClassFile += ((configuredClass._1, methods))
      }
    }

    val modifiedClassFiles = methodsByClassFile.map(c => ClassWriter.mergeMethodsIntoNewClass(c._2, c._1, callGraph))
    val originalClassFiles = deadInstructionsPerMethodExt.map(t => t._1.classFile).toList.distinct

    ResultAnalyzer.analyzeClassFiles(analysisConfig, originalClassFiles, modifiedClassFiles, deadInstructionsPerMethodExt.toMap.mapValues(l => l.map(t => t._1)), slicingResults)

    ClassModificationResult(modifiedClassFiles, deadMethods.size)
  }

  def findDeadMethods(project: Project[URL], libraryClassFiles: LinearSeq[ClassFile], deadInstructionsPerMethodExt: Map[Method, Iterable[(PC, Instruction)]], callGraph: CallGraph): Iterable[Method] = {

    def findMoreDeadMethods(method: Method): Iterable[Method] = {
      val deadMethods = mutable.MutableList[Method]()
      if (libraryClassFiles.contains(method.classFile)) {
        val calledBy = callGraph.calledBy(method)
        if (calledBy.size == 1 && calledBy.head._2.isSingletonSet) {
          deadMethods += method
          val calls = callGraph.calls(method)
          for (call <- calls if call._2.size == 1) {
            deadMethods ++= findMoreDeadMethods(call._2.head)
          }
        }
      }
      deadMethods
    }

    val invocationInstructions = deadInstructionsPerMethodExt.flatMap(t => t._2.map(_._2)).filter(i => i.isMethodInvocationInstruction).map(i => i.asInstanceOf[MethodInvocationInstruction])
    val deadMethods = mutable.MutableList[Method]()
    for (instruction <- invocationInstructions if !instruction.isInterfaceCall) {
      val objectType = instruction.declaringClass.asObjectType
      val classFileOpt = project.classFile(objectType)
      classFileOpt match {
        case Some(classFile) if libraryClassFiles.contains(classFile) =>
          val currentMethodOpt = classFile.methods.find(m => m.name.equals(instruction.name) && m.descriptor.equals(instruction.methodDescriptor))
          currentMethodOpt match {
            case Some(currentMethod) =>
              deadMethods ++= findMoreDeadMethods(currentMethod)
            case None if classFile.superclassType.isDefined =>
              var supertypeMethod: Option[Method] = None
              var supertype: ClassFile = classFile
              breakable {
                do {

                  if (supertype.superclassType.nonEmpty) {
                    val superTypeClassFile = project.classFile(supertype.superclassType.get)
                    if (superTypeClassFile.nonEmpty) {
                      supertype = superTypeClassFile.get
                      supertypeMethod = supertype.methods.find(m => m.name.equals(instruction.name) && m.descriptor.equals(instruction.methodDescriptor))
                    } else {
                      break
                    }
                  } else {
                    break
                  }
                } while (supertypeMethod.isEmpty)
              }

              if (supertypeMethod.nonEmpty)
                deadMethods ++= findMoreDeadMethods(supertypeMethod.get)
          }
        case _ =>
      }
    }

    deadMethods
  }


  def methodEquals(m: Method, m2: Method): Boolean = m.descriptor.equals(m2.descriptor) &&
    m.classFile.fqn.equals(m2.classFile.fqn) && m.name.equals(m2.name)


  def computeCompleteSet(project: Project[URL], methods: Iterable[Method], callGraph: CallGraph, libraryClassFiles: List[ClassFile]): Iterable[Method] = {
    def isLibraryMethod(m: Method): Boolean = {
      if (m == null) return false
      isLibraryClass(m.classFile)
    }

    def isLibraryClass(c: ClassFile): Boolean = {
      if (c == null) return false
      project.allLibraryClassFiles.exists(cl => c.fqn == cl.fqn)
    }


    def computeNecessaryMethods(classes: Set[ClassFile]): Set[Method] = {
      classes.filter(isLibraryClass).flatMap(c => c.methods).filter(m => {
        m.isStaticInitializer
      }) // || (m.isConstructor && m.parametersCount == 1) })
    }


    def computeDependencies(methods: Iterable[Method]): Set[Method] = {

      def isSupertypeMethod(referenceMethod: Method, candidateMethod: Method): Boolean = {
        candidateMethod.hasSignature(referenceMethod.name, referenceMethod.descriptor)
      }

      var result: Set[Method] = Set()
      for (targetMethod <- methods) {
        var superTypes = project.classHierarchy.allSupertypes(ObjectType(targetMethod.classFile.fqn)).map(project.classFile(_).orNull).filter(e => e != null)
        result ++= superTypes.flatMap(c => c.methods).filter(m => isSupertypeMethod(targetMethod, m) && isLibraryMethod(m))
      }

      val usedLibraryAnnotations: Iterable[ClassFile] = methods.flatMap(_.annotations).flatMap(a => project.classFile(a.annotationType.asObjectType)).filter(isLibraryClass)

      result ++= usedLibraryAnnotations.flatMap(c => c.methods)

      result
    }

    def computeFieldAccess(methods: Iterable[Method]): Set[Method] = {
      val pf: PartialFunction[Instruction, Iterable[Method]] = {
        case gs: GETSTATIC => computeNecessaryMethods(Set(project.classFile(gs.declaringClass).orNull))
        case ps: PUTSTATIC => computeNecessaryMethods(Set(project.classFile(ps.declaringClass).orNull))
        case n: NEW => computeNecessaryMethods(Set(project.classFile(n.objectType).orNull))
        case a: NEWARRAY if a.arrayType.elementType.isObjectType => computeNecessaryMethods(Set(project.classFile(a.arrayType.elementType.asObjectType).orNull))
      }

      methods.filter(m => m.body.isDefined).flatMap(m => m.body.get.instructions).collect(pf).flatten.toSet
    }

    def computeStaticInitializer(newCalls: Iterable[Method]): Iterable[Method] = {
      newCalls.flatMap(m => m.classFile.methods).filter(m => m.isStaticInitializer).toList.distinct
    }

    implicit val ec: ExecutionContext = ExecutionContext.global
    val result = new mutable.ListBuffer[Method]
    val workQueue = new mutable.Queue[Method]()
    val startMethods = libraryClassFiles.seq.flatMap(c => c.methods).flatMap(m =>
      callGraph.calledBy(m).
        filter(t => project.allProjectClassFiles.contains(t._1.classFile)).keys)
    workQueue ++= startMethods.toList.distinct.sortBy(m => m.toJava)

    val futureWorkQueue = new mutable.Queue[Future[Iterable[Method]]]()
    futureWorkQueue ++= startMethods.toList.distinct.sortBy(m => m.toJava).map(m => Future({
      getCalls(m)
    }))


    def getCalls(current: Method): Iterable[Method] = {
      var newCalls = callGraph.calls(current).flatMap(_._2)
        .filter(m => isLibraryMethod(m) && !result.exists(m2 => methodEquals(m, m2))).toList.distinct
      newCalls ++= computeNecessaryMethods(newCalls.map(_.classFile).toSet)
      newCalls ++= computeDependencies(newCalls)
      newCalls ++= computeFieldAccess(newCalls)
      newCalls ++= computeStaticInitializer(newCalls).filter(m => !result.exists(m2 => methodEquals(m, m2)))
      newCalls.filter(m => !newCalls.exists(m2 => m != m2 && methodEquals(m, m2)))
    }

    while (futureWorkQueue.nonEmpty) {
      val allCurrents = futureWorkQueue.dequeueAll(_ => true)
      val combinedFuture = Future.sequence(allCurrents)
      val calls = Await.result(combinedFuture, Duration.Inf).flatten.distinct
      val newFutureCalls = calls.map(m => Future(getCalls(m)))
      futureWorkQueue ++= newFutureCalls
      result ++= calls.distinct
    }

    //single threaded variant (better for debugging)
    /*while (workQueue.nonEmpty) {
      val current = workQueue.dequeue()

      var newCalls = callGraph.calls(current).flatMap(_._2).
        filter(m => isLibraryMethod(m) && result.exists(m2 => methodEquals(m, m2))).toList.distinct
      newCalls ++= computeNecessaryMethods(newCalls.map(_.classFile).toSet)
      newCalls ++= computeDependencies(newCalls)
      newCalls ++= computeFieldAccess(newCalls)

      result ++= newCalls.distinct
      workQueue ++= newCalls.distinct
    }*/

    result.distinct
  }

  def getClassesFromConfigFile(project: Project[URL], path: String): Iterable[(ClassFile, Methods)] = {
    val classFilesWithMethods = mutable.MutableList[(ClassFile, Methods)]()
    val file = new File(path)
    if (file.exists() && file.isFile) {
      for (line <- Files.readAllLines(file.toPath).asScala) {
        if (line.contains("*")) {
          val partialClassPath = line.replace(".*", "")
          val classFiles = project.allLibraryClassFiles.filter(c => c.thisType.toJava.contains(partialClassPath))
          classFilesWithMethods ++= classFiles.map(c => (c, c.methods))
        } else if (line.contains("(") && line.contains(")")) {
          val braceIndex = line.indexOf('(')
          val partialClassPath = line.substring(0, braceIndex)
          val classFileOpt = project.allLibraryClassFiles.find(c => c.thisType.toJava.equals(partialClassPath))
          classFileOpt match {
            case Some(classFile) =>
              val methodName = line.substring(braceIndex + 1, line.indexOf(')'))
              val methods = classFile.methods.filter(m => m.name == methodName)
              classFilesWithMethods += ((classFile, methods.toIndexedSeq))
            case _ =>
          }
        } else {
          val firstMatch = project.allLibraryClassFiles.find(c => c.thisType.toJava.equals(line))
          firstMatch match {
            case Some(classFile) =>
              classFilesWithMethods += ((classFile, classFile.methods))
            case _ =>
          }
        }
      }
    }
    classFilesWithMethods
  }

  def getConstantLoadedClasses(project: Project[URL], classFiles: Iterable[ClassFile]): Iterable[ClassFile] = {
    val referenceTypes = classFiles.flatMap(c => c.methods).filter(m => m.body.isDefined).flatMap(m => m.body.get.instructions).collect {
      case LoadClass(t) => t
    }

    referenceTypes.map {
      case o: ObjectType => project.classFile(o)
      case a: ArrayType => if (a.elementType.isObjectType) project.classFile(a.elementType.asObjectType) else None
      case _ => None
    }.collect { case Some(c) => c }.filter(c => project.allLibraryClassFiles.contains(c)).toList.distinct
  }
}