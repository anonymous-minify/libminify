## Contents of this Repository
This repository holds the implementation of an automated library minimization for JVM application stacks, as well as the experiments we conducted to evaluate that implementation. In particular, the top-level directories contain the following files:
* **LibraryMinimizer** Implementation of our approach to automated application stack minimization in Scala. Relies on the SBT for building the project.
* **MavenPlugin** Maven project wrapping the LibraryMinimizer as a Maven plugin.
* **Evaluation** Holds the experiments we conducted in order to answer the three different Research Questions (RQs).
## Building the Maven Plugin
In order to build and publish the Maven Plugin on your local machine, go to the *LibraryMinimizer* directory and execute

    sbt publishM2

Then change to the *MavenPlugin* directory and execute

    mvn install

Now the LibraryMimizer plugin for Maven is available on your local machine.

## Using the Maven Plugin
In order to use our Maven plugin to minimize a project, you need to add it to the projects ```pom.xml``` file. This is done by adding the following snippet to the ```build```-section of that file:

    <plugin>
        <groupId>libraryMinimizer</groupId>
        <artifactId>libraryMinimizerPlugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>
                    desired phase (e.g. test-compile, prepare-package)
                </phase>
            </execution>
        </executions>
        <configuration>            
        </configuration>
    </plugin>

In order to use the Whitelist approach we presented in our paper, create a file called ```LibraryMinimizer.config``` in the root directory of the project that is being minimized. The contents may look like this:

    org.junit.* // all classes that match this package name are kept
    org.junit.Logger // only the Logger class is kept
    org.junit.Logger(log) // all log methods of the Logger class are kept 

## Running Experiments
The folder **Evaluation** contains a subfolder for each of the three RQs that we introduced in our paper. Both *RQ1* and *RQ3* were were performed using Docker containers. In order to reproduce our experiments you have to built the respective image first. Go to the root directory of this repository and execute the following command:

    docker build -f .\Evaluation\RQ1\Dockerfile -t libminify/rq1 .

In order to build the image for RQ3, simply replace both occurrances of *RQ1* with *RQ3*. Both images will, when instanciated as a container, save results to 

    /usr/app/results

In order to make them available on your host machine, you need to map the aforementioned container path to a directory on your host machine. You can then run our experiments for RQ1 by executing

    docker run -v  <path-on-host>:/usr/app/results libminify/rq1

Again, replacing *RQ1* with *RQ3* will run the experiments for RQ3.