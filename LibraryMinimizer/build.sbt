
name := "LibraryMinimizer"
organization := "LibraryMinimizer"
version := "0.1"
scalaVersion := "2.12.8"

publishM2Configuration := publishM2Configuration.value.withOverwrite(true)

libraryDependencies ++= Seq("de.opal-project" % "abstract-interpretation-framework_2.12" % "1.0.0" withSources() withJavadoc(),
  "de.opal-project" % "bytecode-creator_2.12" % "1.0.0" withSources() withJavadoc(),
  "de.opal-project" % "bytecode-assembler_2.12" % "1.0.0" withSources() withJavadoc(),
  "com.novocode" % "junit-interface" % "0.11" % "test" withSources(), /*
  "guru.nidi" % "graphviz-java" % "0.8.0" withSources() withJavadoc(),
  "ch.qos.logback" % "logback-classic" % "1.2.3",*/
  "net.sf.nervalreports" % "html-generator" % "1.1.1" withSources(),
  "org.ow2.asm" % "asm" % "6.0" withSources(),
  "org.ow2.asm" % "asm-util" % "6.0" withSources())

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

enablePlugins(PackPlugin)
packMain := Map("hello" -> "starter.StarterObject.main")

lazy val testLibrary = Project(
  id = "testLibrary",
  base = file("testLibrary"))

lazy val testProject = Project(
  id = "testProject",
  base = file("testProject"))
  .settings().dependsOn(testLibrary)

lazy val branching = Project(
  id = "BranchingTypeAnalysis",
  base = file("BranchingTypeAnalysis"))

lazy val root = Project(id = "LibraryMinimizer", base = file(""))

