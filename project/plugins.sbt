scalacOptions += "-deprecation"

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "cappi" % "0.1.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.1")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.3")

//addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.3")

//addSbtPlugin("com.sksamuel.scoverage" %% "sbt-scoverage" % "0.95.7")