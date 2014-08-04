import scala.xml.Group

lazy val core = project

lazy val json = project in file("codecs/json") dependsOn (core % "compile->compile;test->test")

lazy val jackson = project in file("codecs/jackson") dependsOn (core % "compile->compile;test->test", json % "compile->compile;test->test")

lazy val jawn = project in file("codecs/jawn") dependsOn (core % "compile->compile;test->test", json % "compile->compile;test->test")

lazy val json4s = project in file("codecs/json4s") dependsOn (core % "compile->compile;test->test", json % "compile->compile;test->test")

lazy val strings = project in file("codecs/strings") dependsOn (core % "compile->compile;test->test")

lazy val playJson = project in file("codecs/play-json") dependsOn (core % "compile->compile;test->test", json % "compile->compile;test->test")

lazy val argonaut = project in file("codecs/argonaut") dependsOn (core % "compile->compile;test->test", json % "compile->compile;test->test")

lazy val caliperBenchmarks = project in file("benchmarks/caliper") dependsOn (core % "compile->compile;test->test", jackson % "compile->compile;test->test", json4s % "compile->compile;test->test", playJson % "compile->compile;test->test", argonaut % "compile->compile;test->test", jawn % "compile->compile;test->test", strings % "compile->compile;test->test")

scalaVersion in ThisBuild := "2.10.4"

crossScalaVersions in ThisBuild := Seq("2.10.4", "2.11.1")

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

credentials in ThisBuild ++= ((sys.env.get("SONATYPE_USER"), sys.env.get("SONATYPE_PASS")) match {
  case (Some(user), Some(pass)) =>
    Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass))
  case _ =>
    Seq.empty[Credentials]
})

publishTo in ThisBuild <<= version { version: String =>
  if (version.trim.endsWith("SNAPSHOT"))
    Some(Opts.resolver.sonatypeSnapshots)
  else
    Some(Opts.resolver.sonatypeStaging)
}

publishMavenStyle in ThisBuild := true

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

publishMavenStyle in ThisBuild := true

publishArtifact in Test in ThisBuild := false

pomIncludeRepository in ThisBuild := { x => false }

ivyLoggingLevel in (ThisBuild, update) := UpdateLogging.DownloadOnly

// root project specific settings
if (sys.env.getOrElse("TRAVIS","false").toBoolean) {
  Seq(
    git.remoteRepo := s"https://${sys.env("GH_TOKEN")}@github.com/json4s/muster.git",
    useGpg := false,
    pgpSecretRing := target.value / "secring.gpg",
    pgpPublicRing := target.value / "pubring.gpg",
    pgpPassphrase := sys.env.get("GPG_PASSPHRASE").map(_.toCharArray)
  )
} else 
Seq(
  git.remoteRepo := "git@github.com:json4s/muster.git"
)

publish := {}

publishLocal := {}

PgpKeys.publishSigned := {}

packagedArtifacts := Map.empty

unidocSettings

UnidocKeys.unidocProjectFilter in (ScalaUnidoc, UnidocKeys.unidoc) := inAnyProject -- inProjects(caliperBenchmarks)

scalacOptions in (ScalaUnidoc, UnidocKeys.unidoc) += "-Ymacro-expand:none" // 2.10 => "-Ymacro-no-expand"

site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api")

site.settings

ghpages.settings

site.jekyllSupport()

GhPagesKeys.repository := target.value / "ghpages"


