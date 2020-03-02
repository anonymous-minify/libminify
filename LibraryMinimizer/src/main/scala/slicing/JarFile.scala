package slicing

import java.io.{ByteArrayOutputStream, FileOutputStream}
import java.util.jar.{JarEntry, JarOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import analysis.config.AnalysisConfig
import org.objectweb.asm.ClassReader
import org.opalj.{ba, log}
import org.opalj.ba.ToDAConfig
import org.opalj.bc.Assembler
import org.opalj.br.{ClassFile, ClassHierarchy}
import org.opalj.log._

class JarFile(private val filename: String, private val manifest: java.util.jar.Manifest) {

  private val jarStream = new JarOutputStream(new FileOutputStream(filename), manifest)

  def addClassFile(path: String, classFile: ClassFile, classHierarchy: ClassHierarchy): Unit = {
    try {
      var entryName = path
      val daClass = ba.toDA(classFile)(toDAConfig = ToDAConfig.apply(retainOPALAttributes = false, retainUnknownAttributes = true))
      val bytes = Assembler(daClass)
      val asmClassReader = new ClassReader(bytes)
      val asmClassWriter = new CustomASMClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES, classHierarchy)
      asmClassReader.accept(asmClassWriter, org.objectweb.asm.ClassReader.SKIP_FRAMES)
      val bytesWithStackFrames = asmClassWriter.toByteArray
      if (entryName.startsWith("/")) entryName = entryName.substring(1)
      val entry = new JarEntry(entryName)
      entry.setTime(System.currentTimeMillis / 1000)
      jarStream.putNextEntry(entry)
      jarStream.write(bytesWithStackFrames)
      jarStream.closeEntry()
    } catch {
      case t: Throwable => {
        println(s"Class writing has failed for the following class: ${classFile.fqn}")
        val logMessage = BasicLogMessage(level = Info, t.getStackTrace.mkString)
        log.OPALLogger.log(logMessage)(GlobalLogContext)
      }
    }
  }

  // Add a generic entry to the JAR file
  def addEntry(entry: JarEntry, content: Array[Byte]): Unit = {
    try {
      val newEntry = new JarEntry(entry)
      newEntry.setTime(System.currentTimeMillis / 1000)

      jarStream.putNextEntry(newEntry)
      jarStream.write(content)
      jarStream.closeEntry()

      println(s"Copied non-code resource to target JAR: ${entry.getName}")
    } catch {
      case t: Throwable => {
        println(s"Failed to write entry to JAR: ${entry.getName}")
        println(s"Error ${t.getClass} -> ${t.getMessage}")
        val logMessage = BasicLogMessage(level = Info, t.getStackTrace.mkString)
        log.OPALLogger.log(logMessage)(GlobalLogContext)
      }
    }
  }

  def close(): Unit = {
    jarStream.close()
  }
}
