import scala.xml.Group

lazy val core = project

lazy val json = project in file("codecs/json") dependsOn (core % "compile->compile;test->test")

lazy val strings = project in file("codecs/strings") dependsOn (core % "compile->compile;test->test")

lazy val caliperBenchmarks = project in file("benchmarks/caliper") dependsOn (core % "compile->compile;test->test", json % "compile->compile;test->test", strings % "compile->compile;test->test")

scalaVersion in ThisBuild := "2.11.1"

name := "muster"

organization in ThisBuild := "org.json4s"

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

homepage in ThisBuild := Some(new URL("https://github.com/json4s/muster"))

startYear in ThisBuild := Some(2013)

licenses in ThisBuild := Seq(("MIT", new URL("https://github.com/json4s/muster/raw/HEAD/LICENSE")))

scmInfo in ThisBuild := Some(ScmInfo(url("http://github.com/json4s/muster"), "scm:git:git://github.com/json4s/muster.git", Some("scm:git:git@github.com:json4s/muster.git")))

pomExtra in ThisBuild <<= (pomExtra, name, description) { (pom, name, desc) =>
  pom ++ Group(
     <developers>
        <developer>
          <id>casualjim</id>
          <name>Ivan Porto Carrero</name>
          <url>http://flanders.co.nz/</url>
        </developer>
      </developers>
  )
}

//cappiSettings

//caliperVersion in cappi := Some("1.0-beta-1")
