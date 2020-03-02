package analysis

import java.net.URL

import org.opalj.br.analyses.{Analysis, AnalysisExecutor, BasicReport, ProgressManagement, Project, ReportableAnalysisResult}

/*
  This analysis is only used to gather information about the project statistics such as project instruction count etc.
 */
object StatisticsAnalysis extends Analysis[URL, BasicReport] with AnalysisExecutor {
  override val analysis: Analysis[URL, ReportableAnalysisResult] = this

  override def analyze(project: Project[URL], parameters: Seq[String], initProgressManagement: Int => ProgressManagement): BasicReport = {
    BasicReport(project.statistics.mkString("\n"))
  }
}
