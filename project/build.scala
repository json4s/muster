import sbt._
import Keys._
import com.typesafe.sbt.SbtPgp._
import com.typesafe.sbt.SbtGit._

object MusterBuild {
  val travisCredentials = (sys.env.get("SONATYPE_USER"), sys.env.get("SONATYPE_PASS")) match {
    case (Some(user), Some(pass)) =>
      Seq(Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass))
    case _ =>
      Seq.empty[Credentials]
  }

  val travisSettings: Seq[Setting[_]] =  {
    if (sys.env.getOrElse("TRAVIS","false").toBoolean) {
      println("Executing the travis enabled build")
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
  }
}