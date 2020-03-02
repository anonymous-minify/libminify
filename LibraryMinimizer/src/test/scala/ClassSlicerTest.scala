import org.opalj.ai.analyses.cg.{CallGraphFactory, ExtVTACallGraphAlgorithmConfiguration}
import slicing.ClassSlicer
import org.junit.Assert._

class ClassSlicerTest extends AnalysisTestCase {
  override val classFileName = "ClassSlicerTestCode.class"

  def testMethodRemoval(): Unit = {
    val method = getMethod("main")
    println()
    val deadInstructions = method.body.get.withFilter(t => t._1 >= 5 && t._1 <= 7).map(t => t).toList
    val map = Map((method, deadInstructions))
    val entryPoints = CallGraphFactory.defaultEntryPointsForLibraries(project)
    val computedCallGraph = CallGraphFactory.create(project, () => entryPoints, new ExtVTACallGraphAlgorithmConfiguration(project))
    // for this test scenario we assume that every class file is part of the library
    val deadMethods = ClassSlicer.findDeadMethods(project, project.allClassFiles.toList, map, computedCallGraph.callGraph)
    val expectedDeadMethods = List(classFile.methods(1), classFile.methods(2))
    assertEquals(expectedDeadMethods, deadMethods)
  }
}
