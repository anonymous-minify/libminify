import java.net.URL

import analysis.BranchingTypeAnalysis
import org.opalj.br.analyses.{BasicReport, DefaultOneStepAnalysis, Project}

object AnalysisLauncher extends DefaultOneStepAnalysis {
  override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
    BasicReport(BranchingTypeAnalysis.analyze(project).mkPrettyString)
  }
}
