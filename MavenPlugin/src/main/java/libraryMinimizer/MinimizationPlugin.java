package libraryMinimizer;

import common.BaseMinimizationPlugin;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.opalj.br.analyses.Project;

import java.net.URL;

@Mojo(name = "minimize", requiresDependencyResolution = ResolutionScope.RUNTIME, requiresDependencyCollection = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class MinimizationPlugin extends BaseMinimizationPlugin {

    public void execute() {
        initialize();
        Project<URL> opalProject = getOPALProjectWithClassSources();
        minimize(opalProject);
    }
}


