package evaluation;

import common.BaseEvaluationPlugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.opalj.br.analyses.Project;

import java.io.IOException;
import java.net.URL;

@Mojo(name = "evaluate", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class EvaluationPlugin extends BaseEvaluationPlugin {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initialize();
        Project<URL> opalProject = getOPALProjectWithClassSources();
        try {
            evaluate(opalProject);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
