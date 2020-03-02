package common;

import analysis.MainAnalysis;
import analysis.config.AnalysisConfig;
import analysis.config.DomainTypes;
import analysis.config.ExecutionMode;
import evaluation.MinificationResult;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugins.annotations.Parameter;
import org.opalj.ai.analyses.cg.ComputedCallGraph;
import org.opalj.br.analyses.Project;
import scala.Enumeration;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseMinimizationPlugin extends BasePlugin {
    @Parameter(property = "libraryMinimizer.skipMinimizing")
    private boolean skipMinimizing;

    @Parameter(property = "executionMode", defaultValue = "ExecuteAll")
    private String executionMode;

    protected void minimize(Project<URL> opalProject) {
        String outputPath = getJarOutputDirectory();
        File outputDir = new File(outputPath);
        outputDir.mkdirs();

        if (!skipMinimizing) {
            String configPath = getConfigPath();
            ComputedCallGraph callGraph = getCallGraph(opalProject);
            Enumeration.Value execMode = ExecutionMode.ExecuteAll();
            if (executionMode.equals("ExecuteAnalysis")) {
                execMode = ExecutionMode.ExecuteAnalysis();
            } else if (executionMode.equals("ExecutePackaging")) {
                execMode = ExecutionMode.ExecutePackaging();
            }

            AnalysisConfig config = new AnalysisConfig(false, outputPath, configPath, "", 10, DomainTypes.PreciseDomain(), execMode);
            MinificationResult result = MainAnalysis.analyze(opalProject, opalProject.allLibraryClassFiles(), callGraph.callGraph(), config);
            log.info(result.toString());
        }

        Set<File> set = new HashSet<File>();
        File[] files = outputDir.listFiles();

        Set dependencies = project.getArtifacts();
        if (files != null) {
            Set<DefaultArtifact> newArtifacts = new HashSet<>();
            for (Object d : dependencies) {
                if (d instanceof DefaultArtifact) {
                    DefaultArtifact artifact = (DefaultArtifact) d;
                    File file = Arrays.stream(files).filter(f -> f.getName().equals((artifact.getFile().getName()))).findFirst().orElse(null);
                    if (file != null) {
                        try {
                            artifact.setFile(file);
                            artifact.setResolved(false);
                            newArtifacts.add(artifact);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                }
            }
            project.setArtifacts(newArtifacts);
        }
    }
}

