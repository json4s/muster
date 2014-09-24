import sbt._

object Dependencies {
  val Specs2                   = "org.specs2"                   %% "specs2"                 % "2.3.13"
  val ScalaCheck               = "org.scalacheck"               %% "scalacheck"             % "1.11.3"
  val ScalaMeter               = "com.github.axel22"            %% "scalameter"             % "0.5-M2"
  val Json4sAST                = "org.json4s"                   %% "json4s-ast"             % "3.2.10"
  val Json4sNative             = "org.json4s"                   %% "json4s-native"          % "3.2.10"
  val Json4sJackson            = "org.json4s"                   %% "json4s-jackson"         % "3.2.10"
  val JacksonCore              = "com.fasterxml.jackson.core"   %  "jackson-core"           % "2.4.1.1"
  val JacksonDatabind          = "com.fasterxml.jackson.core"   %  "jackson-databind"       % "2.4.1.3"
  val JacksonScala             = "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.4.1"
  val JsonSmart                = "net.minidev"                  %  "json-smart"             % "2.0-RC3"
  def scalaReflect(sv: String) = "org.scala-lang"               %  "scala-reflect"          % sv
  val Argonaut                 = "io.argonaut"                  %% "argonaut"               % "6.0.4"
  val PlayJson                 = "com.typesafe.play"            %% "play-json"              % "2.3.2"

}