package libraryMinimizer;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import common.BaseMinimizationPlugin;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.opalj.br.analyses.Project;

import java.net.URL;

@Mojo(name = "minimizeTest", requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST, defaultPhase = LifecyclePhase.TEST_COMPILE)
public class MinimizationTestPlugin extends BaseMinimizationPlugin {

    public void execute() {
        initialize();
        Project<URL> opalProject = getOPALProjectWithAllSources();
        minimize(opalProject);
    }
}


