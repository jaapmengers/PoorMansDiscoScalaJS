enablePlugins(ScalaJSPlugin)

name := "PoorMansDisco ScalaJS"

scalaVersion := "2.11.5" // or any other Scala version >= 2.10.2
scalaJSStage in Global := FastOptStage

libraryDependencies += "org.monifu" %%% "monifu" % "0.15.0"
jsDependencies += ProvidedJS / "midiFFI.js"
autoCompilerPlugins := true