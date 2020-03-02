import analysis.DeadCodeAnalysis
import org.junit.Assert._

class DeadCodeAnalysisTest extends AnalysisTestCase {

  val classFileName = "DeadCodeAnalysisTestCode.class"

  def testDoubleCompare = {
    testIf("testDoubleCompare", 3, 36)
  }

  def testIntCompare = {
    testIf("testIntCompare", 3, 35)
  }

  def testLongCompare = {
    testIf("testLongCompare", 3, 36)
  }

  def testIf(methodName: String, low: Int, high: Int): Unit = {
    val method = getMethod(methodName)
    val aiResult = method.body.get.withFilter(t => t._1 > low && t._1 < high).map(t => t).toList
    val deadInstructions = DeadCodeAnalysis(aiResult, method)
    val expectedResult = method.body.get.withFilter(t => t._1 < high).map(t => t)
    assertEquals(expectedResult, deadInstructions)
  }

  def testNestedIf(): Unit = {
    val method = getMethod("testNestedIf")
    val aiResult = method.body.get.withFilter(t => t._1 > 2 && t._1 < 75).map(t => t).toList
    val deadInstructions = DeadCodeAnalysis(aiResult, method)
    val expectedResult = method.body.get.withFilter(t => t._1 < 75).map(t => t)
    assertEquals(expectedResult, deadInstructions)
  }

  def testMultipleNestedIf(): Unit = {
    val method = getMethod("testMultipleNestedIf")
    val aiResult = method.body.get.withFilter(t => (t._1 > 36 && t._1 < 69) ||(t._1 > 84 && t._1 < 115) ).map(t => t).toList
    val deadInstructions = DeadCodeAnalysis(aiResult, method)
    val expectedResult = method.body.get.withFilter(t => (t._1 > 33 && t._1 < 69) || (t._1 > 73 && t._1 < 77) || (t._1 > 84 && t._1 < 115)).map(t => t)
    assertEquals(expectedResult, deadInstructions)
  }
}


