package statistics;

import common.BasePlugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.opalj.br.analyses.Project;

import java.net.URL;

@Mojo(name = "statistics", requiresDependencyResolution = ResolutionScope.NONE, requiresDependencyCollection = ResolutionScope.NONE)
public class StatisticsPlugin extends BasePlugin {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
       Project<URL> opalProject =  getOPALProjectWithAllSources();
       log.info(opalProject.statistics().mkString("\n"));
    }
}
