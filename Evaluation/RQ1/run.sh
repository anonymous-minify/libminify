#!/bin/bash
#Install Maven Plugin
mvn -f /usr/app/MavenPlugin/pom.xml install

mvn -f testProjects/pdfbox/pdfbox/pom.xml compile
mvn -f testProjects/pdfbox/pdfbox/pom.xml libraryMinimizerPlugin:evaluate@evaluate -DresultPath=/usr/app/results -DevaluationMode=EvaluateDomains -e

mvn -f testProjects/tutorials/pom.xml install -P minimizing
mvn -f testProjects/tutorials/pom.xml libraryMinimizerPlugin:evaluateTest@evaluateTest -DresultPath=/usr/app/results -DevaluationMode=EvaluateDomains -P minimizing -e

mvn -f testProjects/guava/pom.xml install -Dmaven.test.skip=true
mvn -f testProjects/guava/guava-tests/pom.xml test-compile 
mvn -f testProjects/guava/guava-tests/pom.xml libraryMinimizerPlugin:evaluateTest@evaluateTest -DresultPath=/usr/app/results  -DevaluationMode=EvaluateDomains -e