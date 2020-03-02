package evaluation

import org.opalj.br.{Code, Method}

/*
  This class is used to compare the original method and the sliced method based on the number of instructions both methods have.
 */
class MethodComparison(val originalMethod: Method, val modifiedMethod: Method) {
  def instructionReduction(): Double = {
    val emptyCode = Code(0, 0, Array())
    1 - (modifiedMethod.body.getOrElse[Code](emptyCode).instructions.length.toDouble / originalMethod.body.get.instructions.length)
  }

  def instructionReductionPercentage: String = s"${instructionReduction() * 100}%"
}
