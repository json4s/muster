import Dependencies._

organization := "org.json4s.muster.codec"

name := "muster-codec-json"

libraryDependencies ++= Seq(JacksonCore, JacksonDatabind)

libraryDependencies ++= Seq(Json4sNative, Json4sJackson).map(_ % "test")

libraryDependencies += JsonSmart % "test"