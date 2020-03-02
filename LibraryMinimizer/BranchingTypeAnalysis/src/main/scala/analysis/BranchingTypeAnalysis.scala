package analysis

import java.net.URL

import org.opalj.ai.InterruptableAI
import org.opalj.br.analyses.Project

import collection.JavaConverters._

object BranchingTypeAnalysis {
  def analyze(project: Project[URL]) = {

    val methods = project.allMethods.par // parallel collection
    val allTypes = new java.util.concurrent.ConcurrentLinkedQueue[BranchType]()
    val calledMethods = new java.util.concurrent.ConcurrentLinkedQueue[CalledMethodResult]()
    val uncoveredInstructions = new java.util.concurrent.ConcurrentLinkedQueue[Class[_]]()
    for (method <- methods if method.body.nonEmpty) {
      val domain = new AnalysisDomain(project, method)
      val ai = new InterruptableAI[AnalysisDomain]
      val result = ai(method, domain)
      result.domain.branchingTypes.foreach(t => allTypes.add(t))
      result.domain.calledMethodsWithType.foreach(c => calledMethods.add(c))
      result.domain.uncoveredInstructions.foreach(c => uncoveredInstructions.add(c))
    }

    implicit val ord = Ordering[Int].reverse
    val instructions = uncoveredInstructions.asScala.toList.distinct
    val groupedTypes = allTypes.asScala.groupBy(t => t).mapValues(_.size).toList.sortBy(_._2).map(t => BranchTypeCount(t._1, t._2))
    val groupedMethods = calledMethods.asScala.groupBy(m => m).mapValues(_.size).toList.sortBy(_._2).map(t => CalledMethodResultCount(t._1, t._2))
    new BranchingTypeResult(groupedTypes, groupedMethods)
  }
}

class BranchingTypeResult(val types: Iterable[BranchTypeCount], val calledMethods: Iterable[CalledMethodResultCount]) {
  def mkPrettyString: String = {
    def round(value: Double): Double = {
      val roundBy = 3
      val w = math.pow(10, roundBy)
      (value * w).toLong.toDouble / w
    }

    val typeCount = types.map(_.count).sum
    val methodsCount = calledMethods.map(_.count).sum
    val typeString = types.map(t => s"${t.toString} (${round((t.count.toDouble / typeCount) * 100)}%)").mkString("\n")
    val methodString = calledMethods.map(t => s"${t.toString} (${round((t.count.toDouble / methodsCount) * 100)}%)").mkString("\n")

    s"Relevant types: \n$typeString\n\n" +
      s"Called methods: \n$methodString\n\n"
  }
}