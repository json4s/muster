import scala.xml.Group
import cappi.Plugin.cappiSettings
import cappi.Keys._
import sbtbuildinfo.Plugin._
import org.sbtidea.SbtIdeaPlugin._

scalaVersion := "2.10.3"

name := "muster"

organization := "com.github.casualjim"

buildInfoSettings

buildInfoPackage := "muster"

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

sourceGenerators in Compile <+= buildInfo

ideaBasePackage := Some("muster")

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

scalacOptions ++= Seq("-target:jvm-1.7", "-unchecked", "-deprecation", "-optimize", "-feature", "-Yinline-warnings")

javacOptions ++= Seq("-deprecation", "-Xlint")

//scalacOptions += "-Ymacro-debug-lite"

//scalacOptions += "-Xlog-implicits"

//scalacOptions ++= Seq("-Xprint:parser", "-Ystop-after:parser", "-Yshow-trees-compact")

//scalacOptions += "-Yshow-trees-compact"

// addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

//libraryDependencies += "com.github.axel22" %% "scalameter" % "0.4" % "test"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.7" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.11.1" % "test"

libraryDependencies += "nf.fr.eraasoft" % "objectpool" % "1.1.2"

libraryDependencies += "joda-time" % "joda-time" % "2.3"

libraryDependencies += "org.joda" % "joda-convert" % "1.5"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.6" % "test"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.6" % "test"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.3.1"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.1"

libraryDependencies += "net.minidev" % "json-smart" % "2.0-RC3" % "test"

//testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

//logBuffered := false

initialCommands in console := """
                                |import muster._
                                |import scala.reflect.runtime.{universe => u}
                                |def read[T](source: String)(implicit rdr: Readable[T]) = rdr.readFormated(source, Muster.from.JsonString)
                              """.stripMargin

initialCommands in (Test, console) := """
                                        |import muster._
                                        |import scala.reflect.runtime.{universe => u}
                                      """.stripMargin

packageOptions <+= (name, version, organization) map {
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

publishTo <<= (version) { version: String =>
  if (version.trim.endsWith("SNAPSHOT"))
    Some(Opts.resolver.sonatypeSnapshots)
  else
    Some(Opts.resolver.sonatypeStaging)
}

val mavenCentralFrouFrou = Seq(
    homepage := Some(new URL("https://github.com/casualjim/muster")),
    startYear := Some(2013),
    licenses := Seq(("MIT", new URL("https://github.com/casualjim/muster/raw/HEAD/LICENSE"))),
    pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
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
    )}
  )

cappiSettings

//caliperVersion in cappi := Some("1.0-beta-1")