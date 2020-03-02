package common;

import analysis.MainAnalysis;
import analysis.config.AnalysisConfig;
import analysis.config.DomainTypes;
import analysis.config.ExecutionMode;
import com.opencsv.CSVWriter;
import evaluation.MinificationResult;
import org.apache.maven.plugins.annotations.Parameter;
import org.opalj.ai.analyses.cg.ComputedCallGraph;
import org.opalj.br.analyses.Project;
import scala.Enumeration;
import scala.collection.JavaConverters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseEvaluationPlugin extends BasePlugin {

    @Parameter(property = "resultPath", defaultValue = "results")
    private String resultPath;

    @Parameter(property = "evaluationMode", defaultValue = "EvaluateDomains")
    private String evaluationMode;

    @Parameter(property = "repetitionCount", defaultValue = "5")
    private Integer repetitionCount;


    protected void evaluate(Project<URL> opalProject) throws IOException {
        ComputedCallGraph computedCallGraph = getCallGraph(opalProject);

        String jarOutputPath = getJarOutputDirectory();
        new File(jarOutputPath).mkdirs();

        String finalResultPath = getFilePath(resultPath, project.getName().replaceAll("[^\\s\\w.-]", "_"), dateFormatter.format(LocalDateTime.now()));

        new File(finalResultPath).mkdirs();

        String statistics = opalProject.statistics().mkString("\n");
        Files.write(Paths.get(finalResultPath, "statistics.txt"), statistics.getBytes());

        if (evaluationMode.equals("EvaluateDomains")) {
            evaluateDomains(opalProject, computedCallGraph, jarOutputPath, finalResultPath);
        } else if (evaluationMode.equals("EvaluatePackaging")) {
            evaluatePackaging(opalProject, computedCallGraph, jarOutputPath, finalResultPath);
        }
    }

    private void evaluatePackaging(Project<URL> opalProject, ComputedCallGraph computedCallGraph, String jarOutputPath, String finalResultPath) {
        ArrayList<String[]> data = new ArrayList<>();

        AnalysisConfig analysisConfig = new AnalysisConfig(false, jarOutputPath, getConfigPath(), finalResultPath, 10, DomainTypes.PreciseDomain(), ExecutionMode.ExecuteAll());
        AnalysisExecutionResult result = doAnalysisWithRepetition(opalProject, computedCallGraph, analysisConfig);
        ArrayList<MinificationResult> executionResults = result.getExecutionResults();
        double avgSizeReduction = executionResults.stream().mapToDouble(MinificationResult::applicationSizeReduction).average().orElse(0);
        double avgDeadMethodCount = executionResults.stream().mapToDouble(MinificationResult::analysisDeadMethodCount).average().orElse(0);
        String[] array = new String[]{"ExecuteAll", String.valueOf(result.getAvgExecutionTime()), String.valueOf(avgSizeReduction), String.valueOf(avgDeadMethodCount)};
        data.add(array);

        analysisConfig = new AnalysisConfig(false, jarOutputPath, getConfigPath(), finalResultPath, 10, DomainTypes.PreciseDomain(), ExecutionMode.ExecutePackaging());
        result = doAnalysisWithRepetition(opalProject, computedCallGraph, analysisConfig);
        executionResults = result.getExecutionResults();

        avgSizeReduction = executionResults.stream().mapToDouble(MinificationResult::applicationSizeReduction).average().getAsDouble();
        array = new String[]{"ExecutePackaging", String.valueOf(result.getAvgExecutionTime()), String.valueOf(avgSizeReduction), String.valueOf(0)};
        data.add(array);

        String filePath = getFilePath(finalResultPath, "PackagingResult-" + dateFormatter.format(LocalDateTime.now()) + ".csv");
        String[] header = {"mode", "executionTime", "reduction", "analysisDeadMethodCount"};
        writeCSV(filePath, header, data);
    }

    private void evaluateDomains(Project<URL> opalProject, ComputedCallGraph computedCallGraph, String jarOutputPath, String finalResultPath) {
        ArrayList<String[]> data = new ArrayList<>();
        Enumeration.ValueSet set = DomainTypes.values();
        List values = JavaConverters.seqAsJavaList(set.toIndexedSeq());
        for (Object value : values) {
            Enumeration.Value enumValue = (Enumeration.Value) value;
            String domainResultPath = getFilePath(finalResultPath, value.toString());
            new File(domainResultPath).mkdirs();

            AnalysisConfig analysisConfig = new AnalysisConfig(false, jarOutputPath, getConfigPath(), domainResultPath, 10, enumValue, ExecutionMode.ExecuteAnalysis());
            AnalysisExecutionResult result = doAnalysisWithRepetition(opalProject, computedCallGraph, analysisConfig);
            ArrayList<MinificationResult> executionResults = result.getExecutionResults();
            double avgExecutionTime = executionResults.stream().mapToDouble(MinificationResult::analysisExecutionTime).average().getAsDouble();
            double avgDeadInstructions = executionResults.stream().mapToDouble(MinificationResult::deadInstructionCount).average().getAsDouble();
            double avgVisitedMethodCount = executionResults.stream().mapToDouble(MinificationResult::visitedMethodCount).average().getAsDouble();
            double avgAnalyzedInstructionCount = executionResults.stream().mapToDouble(MinificationResult::analyzedInstructionCount).average().getAsDouble();
            double avgFailedMethods = executionResults.stream().mapToDouble(MinificationResult::failedMethodCount).average().getAsDouble();
            double avgTotalFailedMethods = executionResults.stream().mapToDouble(MinificationResult::totalFailedMethodResults).average().getAsDouble();
            String[] array = new String[]{enumValue.toString(), String.valueOf(avgExecutionTime), String.valueOf(avgDeadInstructions), String.valueOf(avgAnalyzedInstructionCount), String.valueOf(avgVisitedMethodCount), String.valueOf(avgFailedMethods), String.valueOf(avgTotalFailedMethods)};
            data.add(array);
        }

        String filePath = getFilePath(finalResultPath, "AnalysisResult-" + dateFormatter.format(LocalDateTime.now()) + ".csv");
        String[] header = {"domain", "analysisExecutionTime", "deadInstructionsCount", "analyzedInstructionsCount", "visitedMethodsCount", "failedMethodCount", "totalFailedResultCount"};
        writeCSV(filePath, header, data);
    }

    private AnalysisExecutionResult doAnalysisWithRepetition(Project<URL> opalProject, ComputedCallGraph computedCallGraph, AnalysisConfig analysisConfig) {
        ArrayList<MinificationResult> results = new ArrayList<>();
        ArrayList<Double> measuredTimes = new ArrayList<Double>();
        for (int i = 1; i <= repetitionCount; i++) {
            log.info(String.format("Run (%d of %d): Project %s and domain %s", i, repetitionCount, project.getName(), analysisConfig.domainType().toString()));
            long startTime = System.nanoTime();
            MinificationResult result = MainAnalysis.analyze(opalProject, opalProject.allLibraryClassFiles().toList(), computedCallGraph.callGraph(), analysisConfig);
            long endTime = System.nanoTime();
            measuredTimes.add((endTime - startTime) / 1E9);
            results.add(result);
        }

        double avgTime = measuredTimes.stream().mapToDouble(d -> d).average().getAsDouble();
        AnalysisExecutionResult result = new AnalysisExecutionResult(avgTime, results);
        return result;
    }

    private void writeCSV(String path, String[] header, Iterable<String[]> data) {
        try {
            File file = new File(path);
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile, ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            // adding header to csv
            writer.writeNext(header);
            writer.writeAll(data);
            // closing writer connection
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
