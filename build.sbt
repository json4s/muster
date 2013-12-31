import scala.xml.Group

scalaVersion := "2.10.3"

name := "muster"

organization := "com.github.casualjim"

//buildInfoPackage := "com.github.casualjim"

scalacOptions ++= Seq("-target:jvm-1.7")

//scalacOptions += "-Ymacro-debug-lite"

//scalacOptions += "-Xlog-implicits"

//scalacOptions ++= Seq("-Xprint:parser", "-Ystop-after:parser", "-Yshow-trees-compact")

//scalacOptions += "-Yshow-trees-compact"

// addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies += "com.github.axel22" %% "scalameter" % "0.4" % "test"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.7" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.11.1" % "test"

libraryDependencies += "nf.fr.eraasoft" % "objectpool" % "1.1.2"

libraryDependencies += "joda-time" % "joda-time" % "2.3"

libraryDependencies += "org.joda" % "joda-convert" % "1.5"

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

logBuffered := false

initialCommands in console := """
import scala.reflect.runtime.{universe => u}
"""

initialCommands in (Test, console) := """
import scala.reflect.runtime.{universe => u}
"""

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