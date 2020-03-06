#!/bin/bash
#Install Maven Plugin
mvn -f /usr/app/MavenPlugin/pom.xml install

mvn -f testProjects/tutorials/pom.xml install -P minimizing 
mvn -f testProjects/tutorials/pom.xml libraryMinimizerPlugin:evaluateTest@evaluateTest -DresultPath=/usr/app/results -DevaluationMode=EvaluatePackaging -P minimizing -e

tar czf ./exitcode.tar.gz ./testProjects/tutorials/exitcode/target
mkdir -p /usr/app/results/exitcode/target
cp -f ./exitcode.tar.gz /usr/app/results/exitcode/target/

tar czf ./typometer.tar.gz ./testProjects/tutorials/typometer/target
mkdir -p '/usr/app/results/Typometer/target/'
cp -f ./typometer.tar.gz '/usr/app/results/Typometer/target/'

tar czf ./jadventure.tar.gz ./testProjects/tutorials/jadventure/target
mkdir -p '/usr/app/results/Java Text Adventure/target/'
cp -f ./jadventure.tar.gz '/usr/app/results/Java Text Adventure/target/'

tar czf ./github_api_examples.tar.gz ./testProjects/tutorials/github-api-examples/target
mkdir -p '/usr/app/results/GitHub Java API Examples/target/'
cp -f ./github_api_examples.tar.gz '/usr/app/results/GitHub Java API Examples/target/'

tar czf ./swing_tutorials.tar.gz ./testProjects/tutorials/swing-tutorials/target
mkdir -p '/usr/app/results/swing-tutorials/target/'
cp -f ./swing_tutorials.tar.gz '/usr/app/results/swing-tutorials/target/'

tar czf ./jpass.tar.gz ./testProjects/tutorials/jpass/target
mkdir -p '/usr/app/results/JPass/target/'
cp -f ./jpass.tar.gz '/usr/app/results/JPass/target/'

tar czf ./vertex_examples.tar.gz ./testProjects/tutorials/maven-simplest/target
mkdir -p '/usr/app/results/maven-simplest/target/'
cp -f ./vertex_examples.tar.gz '/usr/app/results/maven-simplest/target/'