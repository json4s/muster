import Dependencies._

name := "muster-codec-json"

libraryDependencies ++= Seq(Json4sNative, Json4sJackson).map(_ % "test")

libraryDependencies += JsonSmart % "test"

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
