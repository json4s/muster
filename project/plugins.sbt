scalacOptions += "-deprecation"

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.1")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.0")

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "cappi" % "0.1.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.1")

//addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.3")

//addSbtPlugin("com.sksamuel.scoverage" %% "sbt-scoverage" % "0.95.7")