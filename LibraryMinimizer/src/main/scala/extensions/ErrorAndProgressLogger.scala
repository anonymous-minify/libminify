package extensions

import org.opalj.log._


class ErrorAndProgressLogger() extends OPALLogger {
  override def log(message: LogMessage)(implicit ctx: LogContext): Unit = {
    message.level match {
      case Error if message.category.isDefined && !message.category.get.equals("project configuration") => printMessage(message)
      case Info if message.category.isDefined && message.category.get.equals("progress") => printMessage(message)
      case _ =>
    }
  }

  def printMessage(message: LogMessage): Unit = {
    println(message.toConsoleOutput(true))
  }
}
