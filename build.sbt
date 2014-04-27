import scala.xml.Group

lazy val core = project

lazy val json = project in file("codecs/json") dependsOn core

lazy val strings = project in file("codecs/strings") dependsOn core

scalaVersion in ThisBuild := "2.11.0"

name := "muster"

organization := "org.json4s"

scalacOptions in ThisBuild ++= Seq("-target:jvm-1.7", "-unchecked", "-deprecation", "-optimize", "-feature", "-Yinline-warnings")

javacOptions in ThisBuild ++= Seq("-deprecation", "-Xlint")

libraryDependencies in ThisBuild ++= Seq(
  Dependencies.Specs2 % "test",
  Dependencies.ScalaCheck % "test",
  Dependencies.ScalaMeter % "test"
)

testOptions in ThisBuild += Tests.Argument(TestFrameworks.Specs2, "console", "junitxml")

testFrameworks in ThisBuild += new TestFramework("org.scalameter.ScalaMeterFramework")

parallelExecution in (ThisBuild, test) := false

logBuffered in ThisBuild := false
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

packageOptions in ThisBuild <+= (name, version, organization) map {
  (title, version, vendor) =>
    Package.ManifestAttributes(
      "Created-By" -> "Simple Build Tool",
      "Built-By" -> System.getProperty("user.name"),
      "Build-Jdk" -> System.getProperty("java.version"),
      "Specification-Title" -> title,
      "Specification-Version" -> version,
      "Specification-Vendor" -> vendor,
      "Implementation-Title" -> title,
      "Implementation-Version" -> version,
      "Implementation-Vendor-Id" -> vendor,
      "Implementation-Vendor" -> vendor
    )
}

publishTo in ThisBuild <<= version { version: String =>
  if (version.trim.endsWith("SNAPSHOT"))
    Some(Opts.resolver.sonatypeSnapshots)
  else
    Some(Opts.resolver.sonatypeStaging)
}

val mavenCentralFrouFrou = Seq(
  homepage := Some(new URL("https://github.com/casualjim/muster")),
  startYear := Some(2013),
  licenses := Seq(("MIT", new URL("https://github.com/casualjim/muster/raw/HEAD/LICENSE"))),
  pomExtra <<= (pomExtra, name, description) { (pom, name, desc) =>
    pom ++ Group(
      <scm>
        <url>http://github.com/casualjim/muster</url>
        <connection>scm:git:git://github.com/casualjim/muster.git</connection>
      </scm>
        <developers>
          <developer>
            <id>casualjim</id>
            <name>Ivan Porto Carrero</name>
            <url>http://flanders.co.nz/</url>
          </developer>
        </developers>
    )
  }
)

//cappiSettings

//caliperVersion in cappi := Some("1.0-beta-1")
