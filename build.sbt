enablePlugins(ScalaJSPlugin)

name := "PoorMansDisco ScalaJS"

scalaVersion := "2.11.5" // or any other Scala version >= 2.10.2
scalaJSStage in Global := FastOptStage

resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

libraryDependencies += "org.monifu" %%% "monifu" % "1.0-M1"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"
libraryDependencies += "com.github.japgolly.scalajs-react" %%% "core" % "0.8.2"
libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.2.8"

jsDependencies += "org.webjars" % "react" % "0.12.2" / "react-with-addons.js" commonJSName "React"
jsDependencies += ProvidedJS / "midiFFI.js"
jsDependencies += ProvidedJS / "server.js"
autoCompilerPlugins := true

mainClass in (Compile, run) := Some("poormansdiscoscalajs.server.PoorMansDisco")