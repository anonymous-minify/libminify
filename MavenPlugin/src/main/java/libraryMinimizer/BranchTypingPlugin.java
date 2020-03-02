package libraryMinimizer;

import analysis.BranchTypeCount;
import analysis.BranchingTypeAnalysis;
import analysis.BranchingTypeResult;
import com.opencsv.CSVWriter;
import common.BasePlugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.opalj.br.Method;
import org.opalj.br.analyses.Project;
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
import java.util.stream.Collectors;

@Mojo(name = "branchTyping", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class BranchTypingPlugin extends BasePlugin {

    @Parameter(property = "resultPath", defaultValue = "results")
    private String resultPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initialize();
        Project<URL> opalProject = getOPALProjectWithAllSources();
        BranchingTypeResult result = BranchingTypeAnalysis.analyze(opalProject);
        List<BranchTypeCount> types = JavaConverters.seqAsJavaList(result.types().toIndexedSeq());
        int typeCount = types.stream().mapToInt(BranchTypeCount::count).sum();

        List<Double> doubleList = types.stream().mapToDouble(t -> ((double) t.count() / typeCount) * 100).boxed().collect(Collectors.toList());

        ArrayList<String[]> data = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            data.add(new String[]{types.get(i).branchType().toString(), String.valueOf(doubleList.get(i)), String.valueOf(types.get(i).count())});
        }

        try {
            String content = result.calledMethods().mkString("\n");
            String finalResultPath = getFilePath(resultPath, project.getName().replaceAll("[^\\s\\w.-]", "_"), dateFormatter.format(LocalDateTime.now()));
            new File(finalResultPath).mkdirs();

            Files.write(Paths.get(finalResultPath, "calledMethodResults.txt"), content.getBytes());

            String statistics = opalProject.statistics().mkString("\n");
            List<Method> methods = JavaConverters.seqAsJavaList(opalProject.allMethodsWithBody().toIndexedSeq());
            long instructionsCount = 0;
            for (Method method : methods) {
                instructionsCount += method.body().get().instructionsCount();
            }

            statistics += "AllInstructionCount -> " + instructionsCount;
            Files.write(Paths.get(finalResultPath, "statistics.txt"), statistics.getBytes());

            File outputFile = new File(getFilePath(finalResultPath, "BranchTypingResult-" + dateFormatter.format(LocalDateTime.now()) + ".csv"));
            FileWriter writer = new FileWriter(outputFile);
            CSVWriter csvWriter = new CSVWriter(writer, ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            // adding header to csv
            String[] header = {"branchType", "percentage", "appearanceCount"};
            csvWriter.writeNext(header);
            csvWriter.writeAll(data);
            // closing writer connection
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
