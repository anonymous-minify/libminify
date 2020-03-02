package analysis

import java.io.File
import java.net.URL

import analysis.MainAnalysis.isStartMethod
import analysis.config.{AnalysisConfig, DomainTypes, ExecutionMode}
import com.typesafe.config.{Config, ConfigValueFactory}
import extensions.ErrorAndProgressLogger
import org.opalj.ai.analyses.cg.{CallGraphFactory, ExtVTACallGraphAlgorithmConfiguration}
import org.opalj.br.analyses.{Analysis, AnalysisExecutor, BasicReport, ProgressManagement, Project, ReportableAnalysisResult}
import org.opalj.br.reader.Java8LambdaExpressionsRewriting
import org.opalj.log.{GlobalLogContext, LogContext, OPALLogger}
import org.opalj.{AnalysisMode, AnalysisModes}

object LibraryMinimizerAnalysis extends Analysis[URL, BasicReport] with AnalysisExecutor {
  override def title: String = "AdvancedAnalysis"

  val debugParam = "-debug"
  val outputPathParam = "-outputPath"
  val configPathParam = "-configPath"
  val verboseParam = "-verbose"
  val resultPathParam = "-resultPath"
  val callChainLengthParam = "-callChainLength"
  val domainParam = "-domain"
  val executionModeParam = "-executionMode"
  val allowedParameters = List(debugParam, outputPathParam, configPathParam, verboseParam, resultPathParam, callChainLengthParam, domainParam, executionModeParam)

  override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
    val verbose = parameters.contains("-verbose")
    if (!verbose) {
      useNonVerboseLogger()
    }

    if (parameters.forall(p => allowedParameters.exists(a => p.contains(a))))
      Nil
    else super.checkAnalysisSpecificParameters(parameters)
  }

  def useNonVerboseLogger(): Unit = {
    val errorOnlyLogger: ErrorAndProgressLogger = new ErrorAndProgressLogger
    OPALLogger.updateLogger(GlobalLogContext, errorOnlyLogger)
  }

  override def description: String = "This analysis minimizes the libraries used by an application based on the given application usage context. It uses Abstract Interpretation to identify unused execution paths and instructions. These unused parts are left out in the newly packaged version of the library."


  override def setupProject(cpFiles: Iterable[File], libcpFiles: Iterable[File], completelyLoadLibraries: Boolean, analysisMode: AnalysisMode, fallbackConfiguration: Config)(implicit initialLogContext: LogContext): Project[URL]
  = {
    val rewritingConfigKey = Java8LambdaExpressionsRewriting.Java8LambdaExpressionsRewritingConfigKey
    val config = fallbackConfiguration.withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.FALSE))
      .withValue(Java8LambdaExpressionsRewriting.Java8LambdaExpressionsLogRewritingsConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.FALSE))
    super.setupProject(cpFiles, libcpFiles, completelyLoadLibraries = true, AnalysisModes.LibraryWithOpenPackagesAssumption, config)
  }


  override def analyze(project: Project[URL], parameters: Seq[String], initProgressManagement: Int => ProgressManagement): BasicReport = {
    def getParameterOrDefaultValue(parameterName: String, defaultValue: String): String = {
      val paramOpt = parameters.find(p => p.contains(parameterName))
      paramOpt match {
        case Some(o) =>
          o.split('=')(1)
        case _ =>
          defaultValue
      }
    }

    val debug = parameters.contains(debugParam)
    val outputPath = getParameterOrDefaultValue(outputPathParam, "")
    val configPath = getParameterOrDefaultValue(configPathParam, "LibraryMinimizer.config")
    val resultPath = getParameterOrDefaultValue(resultPathParam, "")
    val callChainLengthString = getParameterOrDefaultValue(callChainLengthParam, "10")
    val callChainLength = callChainLengthString.toInt
    val domainString = getParameterOrDefaultValue(domainParam, DomainTypes.PreciseDomain.toString)
    val domainOpt = withNameOpt[DomainTypes.Value](DomainTypes, domainString)
    val domain = domainOpt match {
      case Some(value) => value
      case _ => DomainTypes.PreciseDomain
    }
    val executionModeString = getParameterOrDefaultValue(executionModeParam, ExecutionMode.ExecuteAll.toString)
    val executionModeOpt = withNameOpt[ExecutionMode.Value](ExecutionMode, executionModeString)
    val executionMode = executionModeOpt match {
      case Some(value) => value
      case _ => ExecutionMode.ExecuteAll
    }
    val config = new AnalysisConfig(debug, outputPath, configPath, resultPath, callChainLength, domain, executionMode)
    val startMethods = project.allProjectClassFiles.toList.par.flatMap(m => m.methods).filter(m => isStartMethod(m)).seq
    val computedCallGraph = CallGraphFactory.create(project, () => startMethods, new ExtVTACallGraphAlgorithmConfiguration(project))
    val result = MainAnalysis.analyze(project, project.allLibraryClassFiles, computedCallGraph.callGraph, config)
    BasicReport(result.toString)
  }

  def withNameOpt[T <: Enumeration#Value](enumeration: Enumeration, s: String): Option[T] = enumeration.values.find(_.toString == s) match {
    case Some(value) => Option(value.asInstanceOf[T])
    case _ => None
  }

  override val analysis: Analysis[URL, ReportableAnalysisResult] = this
}
