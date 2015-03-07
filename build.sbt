enablePlugins(ScalaJSPlugin)

name := "PoorMansDisco ScalaJS"

scalaVersion := "2.11.5" // or any other Scala version >= 2.10.2
scalaJSStage in Global := FastOptStage

libraryDependencies += "org.monifu" %%% "monifu" % "0.15.0"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"

jsDependencies += ProvidedJS / "midiFFI.js"
jsDependencies += ProvidedJS / "server.js"
autoCompilerPlugins := true