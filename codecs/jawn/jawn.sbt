import Dependencies._

name := "muster-codec-jawn"

resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

libraryDependencies += "org.jsawn" %% "jawn-parser" % "0.5.4"

//scalacOptions += "-Ymacro-debug-lite"

//scalacOptions += "-Xlog-implicits"

//scalacOptions ++= Seq("-Xprint:parser", "-Ystop-after:parser", "-Yshow-trees-compact")

//scalacOptions += "-Yshow-trees-compact"

// addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)

//
//initialCommands in console := """
//                                |import muster._
//                                |import scala.reflect.runtime.{universe => u}
//                                |def read[T:Consumer](source: String) = Muster.consume.Json.as[T](source)
//                              """.stripMargin
//
//initialCommands in(Test, console) := """
//                                       |import muster._
//                                       |import scala.reflect.runtime.{universe => u}
//                                       |def read[T:Consumer](source: String) = Muster.consume.Json.as[T](source)
//                                     """.stripMargin
