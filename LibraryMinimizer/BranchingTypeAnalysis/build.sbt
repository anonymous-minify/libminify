name := "BranchingTypeAnalysis"

version := "0.1"
organization := "LibraryMinimizer"
scalaVersion := "2.12.8"
publishM2Configuration := publishM2Configuration.value.withOverwrite(true)
libraryDependencies ++= Seq("de.opal-project" % "abstract-interpretation-framework_2.12" % "1.0.0" withSources() withJavadoc())

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)