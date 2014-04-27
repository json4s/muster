import sbt._

object Dependencies {
  val Specs2                   = "org.specs2"                 %% "specs2"                 % "2.3.11"
  val ScalaCheck               = "org.scalacheck"             %% "scalacheck"             % "1.11.3"
  val ScalaMeter               = "com.github.axel22"          %% "scalameter"             % "0.5-M2"
  val Json4sNative             = "org.json4s"                 %% "json4s-native"          % "3.2.9"
  val Json4sJackson            = "org.json4s"                 %% "json4s-jackson"         % "3.2.9"
  val JacksonCore              = "com.fasterxml.jackson.core" %  "jackson-core"           % "2.3.3"
  val JacksonDatabind          = "com.fasterxml.jackson.core" %  "jackson-databind"       % "2.3.3"
  val JsonSmart                = "net.minidev"                %  "json-smart"             % "2.0-RC3"
  def scalaReflect(sv: String) = "org.scala-lang"             %  "scala-reflect"          % sv
}