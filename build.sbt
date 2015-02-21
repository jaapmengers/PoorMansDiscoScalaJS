enablePlugins(ScalaJSPlugin)

name := "PoorMansDisco ScalaJS"

scalaVersion := "2.11.5" // or any other Scala version >= 2.10.2
scalaJSStage in Global := FastOptStage

jsDependencies += ProvidedJS / "test.js"
autoCompilerPlugins := true