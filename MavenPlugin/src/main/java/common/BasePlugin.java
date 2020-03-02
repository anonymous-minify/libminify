package common;

import analysis.LibraryMinimizerAnalysis;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.opalj.AnalysisModes;
import org.opalj.ai.analyses.cg.CallGraphFactory;
import org.opalj.ai.analyses.cg.ComputedCallGraph;
import org.opalj.ai.analyses.cg.ExtVTACallGraphAlgorithmConfiguration;
import org.opalj.br.Method;
import org.opalj.br.analyses.Project;
import org.opalj.log.GlobalLogContext$;
import scala.collection.JavaConverters;
import scala.collection.mutable.Buffer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;

public abstract class BasePlugin extends AbstractMojo {
    protected BasePlugin(){
        log = getLog();
    }

    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    protected Log log;
    protected DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss");

    protected void initialize() {
        if (project != null) {
            Set dependencies = project.getArtifacts();

            ArrayList<File> libFiles = new ArrayList<File>();
            for (Object d : dependencies) {
                if (d instanceof DefaultArtifact) {
                    DefaultArtifact artifact = (DefaultArtifact) d;
                    File file = artifact.getFile();
                    if (file != null) {
                        log.info(file.getAbsolutePath());
                        libFiles.add(file);
                    }
                }
            }

            String pathname = getLibraryJarsDirectory();
            new File(pathname).mkdirs();

            for (File f : libFiles) {
                File newFile = new File(getFilePath(pathname, f.getName()));
                try {
                    newFile.createNewFile();
                    Files.copy(f.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected Project<URL> getOPALProjectWithAllSources() {
        File classFiles = new File(getClassSourceDirectory());
        File testClassFiles = new File(getTestClassSourceDirectory());
        ArrayList<File> classSources = new ArrayList<>();
        classSources.add(classFiles);
        classSources.add(testClassFiles);
        Project<URL> opalProject = buildOPALProject(classSources);
        return opalProject;
    }

    protected Project<URL> getOPALProjectWithClassSources() {
        File classFiles = new File(getClassSourceDirectory());
        ArrayList<File> classSources = new ArrayList<>();
        classSources.add(classFiles);
        Project<URL> opalProject = buildOPALProject(classSources);
        return opalProject;
    }

    private Project<URL> buildOPALProject(ArrayList<File> classSources) {
        File sourcePath = new File(getLibraryJarsDirectory());
        log.info(sourcePath.getAbsolutePath());
        ArrayList<File> list = new ArrayList<>();
        list.add(sourcePath);
        ArrayList<File> classList = new ArrayList<>();
        classList.addAll(classSources);
        Buffer<File> libFiles = JavaConverters.asScalaBuffer(list);
        Buffer<File> sourceList = JavaConverters.asScalaBuffer(classList);
        ArrayList<String[]> data = new ArrayList<>();
        Config config = ConfigFactory.load();
        LibraryMinimizerAnalysis.useNonVerboseLogger();
        log.info("Building OPAL project");
        org.opalj.br.analyses.Project<URL> opalProject = LibraryMinimizerAnalysis.setupProject(sourceList, libFiles, true, AnalysisModes.LibraryWithOpenPackagesAssumption(), config, GlobalLogContext$.MODULE$);
        return opalProject;
    }

    ComputedCallGraph getCallGraph(Project<URL> opalProject) {
        log.info("Building call graph");
        scala.collection.Iterable<Method> entryPoints = CallGraphFactory.defaultEntryPointsForLibraries(opalProject);
        return CallGraphFactory.create(opalProject, () -> entryPoints, new ExtVTACallGraphAlgorithmConfiguration(opalProject));
    }
    private String getClassSourceDirectory() {
        return getFilePath(outputDirectory.getAbsolutePath(), "classes");
    }

    private String getTestClassSourceDirectory() {
        return getFilePath(outputDirectory.getAbsolutePath(), "test-classes");
    }

    private String getLibraryJarsDirectory() {
        return getFilePath(outputDirectory.getAbsolutePath(), "sourceJars");
    }

    String getJarOutputDirectory() {
        return getFilePath(outputDirectory.getAbsolutePath(), "minimizedJars");
    }

    String getConfigPath() {
        return getFilePath(basedir.getAbsolutePath(), "LibraryMinimizer.config");
    }

    protected String getFilePath(String first, String... more) {
        return Paths.get(first, more).toString();
    }
}
