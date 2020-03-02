package evaluation

case class MinificationResult(analysisExecutionTime: Double,
                              applicationSizeReduction: Double,
                              deadInstructionCount: Int,
                              analyzedInstructionCount: Int,
                              visitedMethodCount: Int,
                              analysisDeadMethodCount: Int,
                              failedMethodCount: Int,
                              totalFailedMethodResults: Int) {
  override def toString: String = {
    s"MinificationResult(executionTime = ${analysisExecutionTime}, sizeReduction = ${applicationSizeReduction}, " +
      s"deadInstructionCount = ${deadInstructionCount}, visitedMethodCount = ${visitedMethodCount}, analyzedInstructionCount = ${analyzedInstructionCount}, analysisDeadMethodCount= ${analysisDeadMethodCount}, " +
      s"failedMethodCount = ${failedMethodCount}, totalFailedMethodResults = ${totalFailedMethodResults})"
  }
}
