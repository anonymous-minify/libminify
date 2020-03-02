package analysis

import java.io.File
import java.net.URL

import analysis.config.{AnalysisConfig, DomainTypes, ExecutionMode}
import analysis.domains._
import analysis.domains.singleLevelDomains.{BaseSingleLevelAnalysisDomain, ImpreciseSingleLevelAnalysisDomain, IntegerRangeSingleLevelDomain, IntegerSetSingleLevelDomain, LongSetSingleLevelDomain, PreciseSingleLevelDomain, PreciseWithIntegerRangeSingleLevelDomain, StringSingleLevelDomain}
import analysis.domains.singledomains._
import evaluation.{MinificationResult, ResultAnalyzer}
import extensions.ExtensionMethods._
import org.opalj.ai
import org.opalj.ai.analyses.cg._
import org.opalj.ai.analyses.{FieldValuesKey, MethodReturnValuesKey}
import org.opalj.ai.util.XHTML
import org.opalj.ai.{PC, _}
import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.Instruction
import org.opalj.br.reader.Java8Framework
import org.opalj.log.{GlobalLogContext, LogContext}
import org.opalj.util.Milliseconds
import slicing.ClassSlicer.methodEquals
import slicing.{ClassSlicer, ClassWriter}

import scala.collection.immutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/*
  This object is the main entry point for the tool. It executes the different phases (analysis, slicing and packaging phases) and adapts the
 */
object MainAnalysis {


  def analyzeStaticInitializers(project: Project[URL], libraryMethods: Iterable[Method]): Map[ObjectType, AIResult] = {
    implicit val logContext: LogContext = project.logContext
    val staticInitializers = libraryMethods.filter(m => m.isStaticInitializer)
    val results = staticInitializers.par.map(m => {
      val fieldValueInformation = project.get(FieldValuesKey)
      val domain = new ImpreciseSingleLevelAnalysisDomain(project, fieldValueInformation, m)
      val ai = new BoundedInterruptableAI[ImpreciseSingleLevelAnalysisDomain](m.body.get, 1, Milliseconds(5000), () => false)(logContext)
      val result: AIResult = ai(m, domain)
      (m.classFile.thisType, result)
    }).toMap.seq
    results
  }

  def calculateSizeReduction(project: Project[URL], newSize: Long): Double = {
    val jars = project.allLibraryClassFiles.map(c => {
      val source = project.source(c).get.toString
      if (source.contains("jar:file"))
        Option(new File(source.substring("jar:file:".length, source.indexOf("!"))))
      else None
    }).distinct

    val oldSize = jars.collect {
      case Some(file) => file.length()
      case _ => 0
    }.sum

    1 - newSize.toDouble / oldSize
  }

  def isStartMethod(m: Method): Boolean = {
    if (m.attributes.nonEmpty) {
      for (attribute <- m.attributes) {
        attribute match {
          case RuntimeVisibleAnnotationTable(annotations) => {
            if (annotations.exists(a => a.annotationType.toJava.contains("junit")) && m.body.nonEmpty)
              return true
          }
          case _ =>
        }
      }
    }

    // ignore lambdas as start methods
    if (m.classFile.isVirtualType || m.isSynthetic) return false

    m.isPublic && !m.isConstructor && m.body.nonEmpty
  }

  def analyzeFailedMethodCalls(failedMethodCalls: immutable.Seq[Method], callGraph: CallGraph, project: Project[URL], config: AnalysisConfig): Iterable[AnalysisResult] = {

    def isLibraryMethod(m: Method): Boolean = {
      if (m == null) return false
      isLibraryClass(m.classFile)
    }

    def isLibraryClass(c: ClassFile): Boolean = {
      if (c == null) return false
      project.allLibraryClassFiles.exists(cl => c.fqn == cl.fqn)
    }

    implicit val ec: ExecutionContext = ExecutionContext.global
    val result = new mutable.ListBuffer[Method]
    val workQueue = new mutable.Queue[Method]()
    val startMethods = failedMethodCalls
    workQueue ++= startMethods.toList.distinct.sortBy(m => m.toJava)

    val futureWorkQueue = new mutable.Queue[Future[Iterable[Method]]]()
    futureWorkQueue ++= startMethods.toList.distinct.sortBy(m => m.toJava).map(m => Future({
      getCalls(m)
    }))


    def getCalls(current: Method): Iterable[Method] = {
      var newCalls = callGraph.calls(current).flatMap(_._2).toList.distinct
        .filter(m => isLibraryMethod(m) && !result.exists(m2 => methodEquals(m, m2))).distinct
      newCalls.filter(m => !newCalls.exists(m2 => m != m2 && methodEquals(m, m2)))
    }

    while (futureWorkQueue.nonEmpty) {
      val allCurrents = futureWorkQueue.dequeueAll(_ => true)
      val combinedFuture = Future.sequence(allCurrents)
      val calls = Await.result(combinedFuture, Duration.Inf).flatten.distinct
      val newFutureCalls = calls.map(m => Future(getCalls(m)))
      futureWorkQueue ++= newFutureCalls
      result ++= calls.distinct.filter(m => !m.isNative)
    }

    val failedResults = result.distinct.par.map(m => executeSingleLevelAnalysis(project, m, config))
      .map(r => {
        val instructions = r.code.withFilter(tuple => !r.wasEvaluted(tuple._1)).map(t => t)
        val deadInstructionsPerMethod: Map[Method, Iterable[(PC, Instruction)]] = Map((r.domain.method, instructions))
        AnalysisResult(deadInstructionsPerMethod, r.evaluatedInstructions.size, List(r.domain.method), Map(), List())
      })
    failedResults.seq

  }

  def analyze(project: Project[URL], libraryClassFiles: Iterable[ClassFile], callGraph: CallGraph, config: AnalysisConfig): MinificationResult = {
    val deadInstructionsPerMethod = mutable.Map[Method, Iterable[(PC, Instruction)]]()
    var mergedUnresolvedCalls: Map[ObjectType, ListBuffer[String]] = Map()
    val visitedMethods = mutable.ListBuffer[Method]()
    var timeInSeconds: Double = 0
    var deadInstructionCount = 0
    var analyzedInstructionsCount = 0
    var analysisDeadMethodCount = 0
    var failedMethodCount = 0
    var totalFailedMethodResults = 0
    if (config.executionMode == ExecutionMode.ExecuteAnalysis || config.executionMode == ExecutionMode.ExecuteAll) {
      val analysisStartTime = System.nanoTime
      val alternativeStartMethods = project.allProjectClassFiles.toList.par.flatMap(m => m.methods).filter(m => isStartMethod(m)).seq.sortBy(m => callGraph.calledBy(m).size)
      val libraryMethods = HashSet(libraryClassFiles.toList.flatMap(c => c.methods): _*)
      val staticInitializerMap = analyzeStaticInitializers(project, libraryMethods)

      val results = alternativeStartMethods.par.map(m => {
        val result = analyzePath(project, m, config, libraryMethods, staticInitializerMap)
        val failedMethods = ListBuffer[Method]()
        if (result.wasAborted && result.domain.failedMethodCalls.nonEmpty) {
          failedMethods += result.domain.failedMethodCalls.head
        }

        AnalysisResult(result.domain.deadInstructionsPerMethod.toMap, result.domain.analyzedInstructionsCount, result.domain.visitedMethods, result.domain.unresolvedInterfaceCalls.toMap, failedMethods)
      }).seq

      val failedMethodCalls = results.flatMap(r => r.failedMethodCalls).distinct.filter(v => libraryMethods.exists(m => m.equals(v))).map(v => project.allLibraryClassFiles.flatMap(_.methods).find(m => m.equals(v)).get).distinct

      val failedMethodResults = analyzeFailedMethodCalls(failedMethodCalls, callGraph, project, config).toList
      failedMethodCount = failedMethodCalls.size
      totalFailedMethodResults = failedMethodResults.size
      val combinedResults = results ++ failedMethodResults

      //val results = mutable.ListBuffer[AIResult {val domain: BaseAnalysisDomain}]()
      val maps = combinedResults.map(r => r.deadInstructionsPerMethod)
      analyzedInstructionsCount = combinedResults.map(r => r.analyzedInstructionsCount).sum + staticInitializerMap.map(t => t._2.evaluatedInstructions.size).sum
      visitedMethods ++= combinedResults.flatMap(r => r.visitedMethods)

      //synchronized version (better for debugging)
      /*val maps = ListBuffer[Map[Method, Iterable[(PC, Instruction)]]]()
      var analyzedInstructionsCount = staticInitializerMap.map(t => t._2.evaluatedInstructions.size).sum
      val visitedMethods = mutable.ListBuffer[Method]()
      for (method <- alternativeStartMethods.filter(m => m.name == "main" && m.classFile.fqn.contains("HelloWorldApplication"))) {
        val result = analyzePath(project, method, config, libraryMethods, staticInitializerMap)
        maps += result.domain.deadInstructionsPerMethod.toMap
        analyzedInstructionsCount += result.domain.analyzedInstructionsCount
        visitedMethods ++= result.domain.visitedMethods
      }*/

      val mergedResults: mutable.Map[Method, Iterable[(PC, Instruction)]] = mergeAIResults(maps)
      mergedUnresolvedCalls = mergeUnresolvedCalls(results.map(r => r.unresolvedInterfaceCalls))


      val analysisEndTime = System.nanoTime

      // do dead code analysis to find remaining unnecessary instructions
      deadInstructionsPerMethod ++= mergedResults.par.map(t => (t._1, DeadCodeAnalysis(t._2.toList, t._1))).seq
      deadInstructionCount = deadInstructionsPerMethod.map(t => t._2.size).sum
      timeInSeconds = (analysisEndTime - analysisStartTime) / 1E9
    }

    var sizeReduction: Double = 0
    if (config.executionMode == ExecutionMode.ExecutePackaging || config.executionMode == ExecutionMode.ExecuteAll) {
      val completeMethodSet = ClassSlicer.computeCompleteSet(project, deadInstructionsPerMethod.keys, callGraph, libraryClassFiles.toList).toList
      val affectedMethods = deadInstructionsPerMethod.keys.filter(method => {
        val supertypes = project.classHierarchy.allSuperinterfacetypes(method.classFile.thisType, reflexive = false)
        supertypes.exists(t => mergedUnresolvedCalls.contains(t) && mergedUnresolvedCalls(t).contains(method.name))
      })
      affectedMethods.foreach(m => deadInstructionsPerMethod.remove(m))
      val newMethods = completeMethodSet.diff(deadInstructionsPerMethod.keys.toList)

      deadInstructionsPerMethod ++= newMethods.map(m => (m, List()))

      val modificationResult = ClassSlicer.sliceClasses(project, config, deadInstructionsPerMethod, callGraph)
      //for now just map to the names of the jars
      val jars = modificationResult.modifiedClassFiles.groupBy(c => {
        val source = project.source(c).get.toString
        if (source.contains("jar:file"))
          Option(new File(source.substring("jar:file:".length, source.indexOf("!"))))
        else None
      })

      val file = org.opalj.bytecode.RTJar
      val classFiles = Java8Framework.ClassFiles(file).map(t => t._1)
      val classHierarchy = ClassHierarchy(project.allClassFiles ++ classFiles, ClassHierarchy.noDefaultTypeHierarchyDefinitions())(GlobalLogContext)
      val outputSize = ClassWriter.writeJarFiles(config.outputPath, project, classHierarchy, jars)
      sizeReduction = calculateSizeReduction(project, outputSize)
      analysisDeadMethodCount = modificationResult.analysisDeadMethodCount
    } else {
      val originalClassFiles = deadInstructionsPerMethod.map(t => t._1.classFile).toList.distinct
      ResultAnalyzer.analyzeClassFiles(config, originalClassFiles, List(), deadInstructionsPerMethod.toMap.mapValues(l => l.map(t => t._1)), List(), printDeadInstructions = true)
    }

    val visitedMethodCount = visitedMethods.distinct.size
    MinificationResult(timeInSeconds, sizeReduction, deadInstructionCount, analyzedInstructionsCount, visitedMethodCount, analysisDeadMethodCount, failedMethodCount, totalFailedMethodResults)
  }

  def analyzePath(project: Project[URL], startMethod: Method, config: AnalysisConfig, libraryMethods: HashSet[Method], staticInitializerMap: Map[ObjectType, AIResult]): AIResult {
    val domain: BaseAnalysisDomain
  } = {
    val result = executeAI(project, startMethod, config, libraryMethods, staticInitializerMap)

    val operandsArray: TheOperandsArray[result.domain.Operands] = result.operandsArray
    val localsArray: TheLocalsArray[result.domain.Locals] = result.localsArray
    if (config.debug) {
      org.opalj.io.writeAndOpen(
        org.opalj.ai.common.XHTML.dump(
          Some(startMethod.classFile),
          Some(startMethod),
          startMethod.body.get,
          Some(
            "Created: " + (new java.util.Date).toString + "<br>" +
              "Domain: " + result.domain.getClass.getName + "<br>" +
              XHTML.evaluatedInstructionsToXHTML(result.evaluated)
          ),
          result.domain
        )(result.cfJoins, operandsArray, localsArray),
        "AIResult",
        ".html"
      )
    }
    result
  }

  def executeAI(project: Project[URL], startMethod: Method, config: AnalysisConfig, libraryMethods: HashSet[Method], staticInitializerMap: Map[ObjectType, AIResult]): AIResult {
    val domain: BaseAnalysisDomain
  } = {
    implicit val logContext: LogContext = project.logContext
    val fieldValueInformation = project.get(FieldValuesKey)
    val returnValueInformation = project.get(MethodReturnValuesKey)
    val cache = new CallGraphCache[MethodSignature, scala.collection.Set[Method]](project)
    val maxEvaluationFactor = 10
    val result: AIResult {
      val domain: BaseAnalysisDomain
    } = config.domainType match {
      case DomainTypes.ImpreciseDomain =>
        val domain = new ImpreciseLibraryMinimizerAnalysisDomain(project, fieldValueInformation, returnValueInformation, cache, config.callChainLength, startMethod, libraryMethods, config.debug, staticInitializerMap)
        val ai = new InstructionCountBoundedAI[ImpreciseLibraryMinimizerAnalysisDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.PreciseWithIntegerRange =>
        val domain = new LibraryMinimizerAnalysisDomainWithIntegerRange(project, fieldValueInformation, returnValueInformation, cache, 5, config.callChainLength, startMethod, libraryMethods, config.debug, staticInitializerMap)
        val ai = new InstructionCountBoundedAI[LibraryMinimizerAnalysisDomainWithIntegerRange](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.IntegerSetDomain =>
        val domain = new LibraryMinimizerAnalysisIntegerSetDomain(project, fieldValueInformation, returnValueInformation, cache, config.callChainLength, startMethod, libraryMethods, config.debug, staticInitializerMap)
        val ai = new InstructionCountBoundedAI[LibraryMinimizerAnalysisIntegerSetDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.IntegerRangeDomain =>
        val domain = new LibraryMinimizerAnalysisIntegerRangeDomain(project, fieldValueInformation, returnValueInformation, cache, config.callChainLength, startMethod, libraryMethods, config.debug, staticInitializerMap)
        val ai = new InstructionCountBoundedAI[LibraryMinimizerAnalysisIntegerRangeDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.LongSetDomain =>
        val domain = new LibraryMinimizerAnalysisLongSetDomain(project, fieldValueInformation, returnValueInformation, cache, 5, config.callChainLength, startMethod, libraryMethods, config.debug, staticInitializerMap)
        val ai = new InstructionCountBoundedAI[LibraryMinimizerAnalysisLongSetDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.StringDomain =>
        val domain = new LibraryMinimizerAnalysisStringDomain(project, fieldValueInformation, returnValueInformation, cache, config.callChainLength, startMethod, libraryMethods, config.debug, staticInitializerMap)
        val ai = new InstructionCountBoundedAI[LibraryMinimizerAnalysisStringDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.PreciseDomain | _ =>
        val domain = new PreciseLibraryMinimizerAnalysisDomain(project, fieldValueInformation, returnValueInformation, cache, 5, config.callChainLength, startMethod, libraryMethods, config.debug, staticInitializerMap)
        val ai = new InstructionCountBoundedAI[PreciseLibraryMinimizerAnalysisDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
    }

    result.domain.analyzedInstructionsCount += result.evaluated.size
    if (!result.wasAborted) {
      result.domain.visitedMethods += startMethod
    }

    result
  }

  def executeSingleLevelAnalysis(project: Project[URL], startMethod: Method, config: AnalysisConfig): AIResult {val domain: BaseSingleLevelAnalysisDomain} = {
    implicit val logContext: LogContext = project.logContext
    val fieldValueInformation = project.get(FieldValuesKey)
    val maxEvaluationFactor = 1
    val result: AIResult {val domain: BaseSingleLevelAnalysisDomain} = config.domainType match {
      case DomainTypes.ImpreciseDomain =>
        val domain = new ImpreciseSingleLevelAnalysisDomain(project, fieldValueInformation, startMethod)
        val ai = new InstructionCountBoundedAI[ImpreciseSingleLevelAnalysisDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.PreciseWithIntegerRange =>
        val domain = new PreciseWithIntegerRangeSingleLevelDomain(project, fieldValueInformation, startMethod)
        val ai = new InstructionCountBoundedAI[PreciseWithIntegerRangeSingleLevelDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.IntegerSetDomain =>
        val domain = new IntegerSetSingleLevelDomain(project, fieldValueInformation, startMethod)
        val ai = new InstructionCountBoundedAI[IntegerSetSingleLevelDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.IntegerRangeDomain =>
        val domain = new IntegerRangeSingleLevelDomain(project, fieldValueInformation, startMethod)
        val ai = new InstructionCountBoundedAI[IntegerRangeSingleLevelDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.LongSetDomain =>
        val domain = new LongSetSingleLevelDomain(project, fieldValueInformation, startMethod)
        val ai = new InstructionCountBoundedAI[LongSetSingleLevelDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.StringDomain =>
        val domain = new StringSingleLevelDomain(project, fieldValueInformation, startMethod)
        val ai = new InstructionCountBoundedAI[StringSingleLevelDomain](startMethod.body.get, maxEvaluationFactor, true)
        ai(startMethod, domain)
      case DomainTypes.PreciseDomain | _ =>
          val domain = new PreciseSingleLevelDomain(project, fieldValueInformation, startMethod)
          val ai = new InstructionCountBoundedAI[PreciseSingleLevelDomain](startMethod.body.get, maxEvaluationFactor, true)
          ai(startMethod, domain)
    }

    result
  }


  def mergeAIResults(results: Iterable[Map[Method, Iterable[(PC, Instruction)]]]): mutable.Map[Method, Iterable[(PC, Instruction)]] = {
    val allMethods = results.flatMap(map => map.keys).toList.distinct
    mutable.Map(allMethods.par.map(method => {
      val pcLists = results.filter(map => map.contains(method)).map(map => map(method)).map(list => list.map(t => t._1).toList)
      var mergedPCs = List[ai.PC]()
      if (pcLists.nonEmpty) {
        mergedPCs = pcLists.head
        pcLists.tail.foreach(i => mergedPCs = mergedPCs.intersect(i))
      }
      val code = method.body.get
      val pcWithInstructions = mergedPCs.map(pc => (pc, code.instructions(pc)))
      (method, pcWithInstructions)
    }).seq: _*)
  }

  def mergeUnresolvedCalls(maps: Iterable[Map[ObjectType, ListBuffer[String]]]): Map[ObjectType, ListBuffer[String]] = {
    val allObjectTypes = maps.flatMap(map => map.keys).toList.distinct
    Map(allObjectTypes.map(objectType => {
      val methods = maps.filter(map => map.contains(objectType)).flatMap(map => map(objectType)).toListBuffer.distinct
      (objectType, methods)
    }).seq: _*)
  }

}
