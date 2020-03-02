package evaluation;

import common.BaseEvaluationPlugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.opalj.br.analyses.Project;

import java.io.IOException;
import java.net.URL;

@Mojo(name = "evaluateTest", requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
public class EvaluationTestPlugin extends BaseEvaluationPlugin {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initialize();
        Project<URL> opalProject = getOPALProjectWithAllSources();
        try {
            evaluate(opalProject);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
