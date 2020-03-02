name := "testProject"
version := "0.1"
scalaVersion := "2.12.8"

mainClass in assembly := Some("MainClass")
enablePlugins(PackPlugin)
packMain := Map("hello" -> "MainClass")
