package slicing

import java.io.File
import java.net.URL
import java.util.jar
import java.util.jar.JarEntry

import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.br.analyses.Project
import org.opalj.br.{ClassFile, ClassHierarchy, MethodTemplate}
import org.opalj.collection.immutable.UShortPair

object ClassWriter {

  def mergeMethodsIntoNewClass(newMethods: Iterable[MethodTemplate], classFile: ClassFile, callGraph: CallGraph): ClassFile = {
    //val remainingMethods = classF.methods.filterNot(m => newMethods.exists(m2 => m2.name == m.name))
    //val copiedMethods = remainingMethods.map(m => m.copy()) ++ newMethods
    //val classFile = classF.copy(methods = copiedMethods.toIndexedSeq

    if (classFile.isVirtualType)
      return classFile

    //the commented section is for the handling of rewritten lambda expressions
    /*val oldMethods = classFile.methods.toList
    val combinedMethodList = (oldMethods.filter(m => !newMethods.toList.exists(m2 => m2.signature.equals(m.signature))).map(m => m.copy())
      ++ newMethods).toListBuffer

    //val syntheticMethods = combinedMethodList.filter(m => m.isSynthetic)
    val lambdaMethods = combinedMethodList.filter(m => callGraph.calledBy(oldMethods.find(m2 => m2.signature.equals(m.signature)).get)
      .exists(t => t._1.classFile.fqn.contains("Lambda")))
    val modifiedLambdaMethods = lambdaMethods.map(m => {
      var accessFlags = m.accessFlags
      //if (m.isPrivate)
      accessFlags = (accessFlags & ~ACC_PRIVATE.mask) | ACC_PUBLIC.mask

      m.copy(accessFlags = accessFlags)
    })


    val combinedSyntheticMethods = combinedMethodList.diff(lambdaMethods).map(m => m.copy()) ++ modifiedLambdaMethods
    combinedMethodList.foreach(m => {
      val m2 = combinedSyntheticMethods.find(m2 => m2.signature.equals(m.signature))
      if (m2.isDefined)
        combinedMethodList.update(combinedMethodList.indexOf(m), m2.get)
    })

    var accessFlags = classFile.accessFlags
    if (modifiedLambdaMethods.nonEmpty) {
      accessFlags = (accessFlags & ~ACC_PRIVATE.mask) | ACC_PUBLIC.mask
    }*/

    val newClassFile = classFile.copy(methods = newMethods.toIndexedSeq)

    newClassFile
  }


  def writeJarFiles(outputPath: String, project: Project[URL], classHierarchy: ClassHierarchy, jarsWithFiles: Map[Option[File], Iterable[ClassFile]]): Long = {
    var size: Long = 0
    for ((optFile, classFiles) <- jarsWithFiles) {
      optFile match {
        case Some(file) =>
          val jarFile = new jar.JarFile(file)

          // Calculate resource files that need to be copied to resulting JAR
          val entries = jarFile.entries()
          var nonClassEntries = Map[JarEntry, Array[Byte]]()

          while(entries.hasMoreElements){
            val entry = entries.nextElement()

            // Persist all entries that are not .class files, not the manifest (is freshly generated) & not a folder
            if(!entry.getName.toLowerCase.endsWith(".class") &&
              !entry.getName.toLowerCase.contains("manifest.mf") &&
              !entry.getName.endsWith("/") &&
              !entry.getName.toLowerCase.endsWith(".zip")){ //Skip .zip files for now, as they seem to invalidate JAR layout

              // Read contents of resource entry from stream
              val stream = jarFile.getInputStream(entry)
              val contents = Stream.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray
              stream.close()

              // Store information about entry in map
              nonClassEntries = nonClassEntries + (entry -> contents)
            }
          }

          val path = s"$outputPath\\${file.getName}"
          writeJarFile(project, classHierarchy, path, classFiles, jarFile.getManifest, nonClassEntries)
          var newFile = new File(path)
          size += newFile.length()
        case None =>
          // classes that don't belong to any jar are bundled into an output.jar in the configured output path
          writeJarFile(project, classHierarchy, s"$outputPath\\output.jar", classFiles, new java.util.jar.Manifest(), Map())
      }
    }
    size
  }

  def writeJarFile(project: Project[URL], classHierarchy: ClassHierarchy, path: String, classes: Iterable[ClassFile], manifest: java.util.jar.Manifest, nonClassEntries: Map[JarEntry, Array[Byte]]): Unit = {
    val jarFile = new JarFile(path, manifest)
    classes.foreach(c => {
      val url = project.source(c).get
      val path = url.toExternalForm
      if (path.contains("jar:file")) {
        if (!c.isVirtualType) {
          jarFile.addClassFile(path.substring(path.indexOf('!') + 1), c, classHierarchy)
        } else {
          var classFile = c
          if (classFile.fqn.contains("Lambda")) {
            classFile = c.copy(version = UShortPair(0, 50))
          }
          jarFile.addClassFile(classFile.fqn + ".class", classFile, classHierarchy)
        }
      }
      else {
        val fileName = new File(url.getFile).getName()
        jarFile.addClassFile(fileName, c, classHierarchy)
      }
    })

    // Write all resource entries to JAR
    nonClassEntries.foreach( pair => {
      jarFile.addEntry(pair._1, pair._2)
    })

    //val sourceClasses = classes.map()
    jarFile.close()
  }

}

