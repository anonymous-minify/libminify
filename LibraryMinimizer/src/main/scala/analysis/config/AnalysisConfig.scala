package analysis.config

class AnalysisConfig(val debug: Boolean,
                     val outputPath: String,
                     val configPath: String,
                     val resultPath: String,
                     val callChainLength: Int,
                     val domainType: DomainTypes.Value,
                     val executionMode: ExecutionMode.Value) {

}


