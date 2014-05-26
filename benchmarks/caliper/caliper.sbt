import cappi.Plugin.cappiSettings
import cappi.Keys._


name := "muster-caliper-benchmarks"

cappiSettings

//caliperVersion in cappi := Some("1.0-beta-1")

publishTo := None

publishMavenStyle := true

publish := {}

publishLocal := {}

PgpKeys.publishSigned := {}