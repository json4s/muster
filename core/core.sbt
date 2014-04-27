import sbtbuildinfo.Plugin._
import Dependencies._

buildInfoSettings

buildInfoPackage := "muster"

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

sourceGenerators in Compile <+= buildInfo

organization := "org.json4s"

name := "muster-core"

//libraryDependencies ++= Seq(JacksonCore, JacksonDatabind)

libraryDependencies += JacksonCore

libraryDependencies += "org.scala-lang" %  "scala-reflect" % scalaVersion.value

compileOrder := CompileOrder.JavaThenScala

//scalacOptions += "-Ymacro-debug-lite"
//
//scalacOptions += "-Xlog-implicits"

//scalacOptions ++= Seq("-Xprint:parser", "-Ystop-after:parser", "-Yshow-trees-compact")

//scalacOptions += "-Yshow-trees-compact"

// addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)
