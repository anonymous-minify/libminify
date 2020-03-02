import java.io.File
import java.net.URL

import extensions.ErrorAndProgressLogger
import junit.framework.TestCase
import org.opalj.br.analyses.Project
import org.opalj.br.{ClassFile, Method}
import org.opalj.log.{GlobalLogContext, OPALLogger}

abstract class AnalysisTestCase extends TestCase {
  private val basePath = "target//scala-2.12//test-classes//testCode//"
  val classFileName: String
  protected var classFile: ClassFile = _
  protected var project: Project[URL] = _

  override def setUp(): Unit = {
    val file = new File(basePath + classFileName)
    OPALLogger.updateLogger(GlobalLogContext, new ErrorAndProgressLogger())
    project = Project(file)
    /*val cfs: List[ClassFile] =
      process(new DataInputStream(new FileInputStream(basePath + classFileName))) { in =>
        org.opalj.br.reader.Java8Framework.ClassFile(in)
      }*/
    classFile = project.allClassFiles.head
  }

  def getMethod(methodName: String): Method = {
    classFile.methods.find(m => m.name.equals(methodName)).get
  }
}
