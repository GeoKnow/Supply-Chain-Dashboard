import com.typesafe.sbt.packager.Keys._
import sbt.Keys._

//////////////////////////////////////////////////////////////////////////////
// Common Settings
//////////////////////////////////////////////////////////////////////////////

lazy val commonSettings = Seq(
  organization := "com.eccenca",
  version := "1.1-SNAPSHOT",
  // Building
  scalaVersion := "2.11.6",
  javacOptions := Seq("-source", "1.7", "-target", "1.7"),
  scalacOptions += "-target:jvm-1.7",
  // Resolvers
  // The Typesafe repository
  resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
  // Testing
  // libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  // libraryDependencies += "junit" % "junit" % "4.11" % "test",
  // testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")
)

//////////////////////////////////////////////////////////////////////////////
// Core Modules
//////////////////////////////////////////////////////////////////////////////

lazy val core = project
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "org.apache.jena" % "jena-core" % "2.11.2" excludeAll ExclusionRule(organization = "org.slf4j"),

      libraryDependencies += "org.apache.jena" % "jena-arq" % "2.11.2" excludeAll ExclusionRule(organization = "org.slf4j"),

      libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.3.7"
    )
    .dependsOn(virtuoso_jena)

//////////////////////////////////////////////////////////////////////////////
// Simulator Modules
//////////////////////////////////////////////////////////////////////////////
lazy val simulator = project
    .enablePlugins(PlayScala)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.4",

      libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.3.7",

      libraryDependencies += "com.typesafe.play" % "play-ws_2.11" % "2.3.7",

      // Linux Packaging, Uncomment to generate Debian packages that register the Workbench as an Upstart service
      packageArchetype.java_server,
      version in Debian := "2.6.1",
      maintainer := "Robert Isele <silk-discussion@googlegroups.com>",
      packageSummary := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources.",
      packageDescription := "The Silk framework is a tool for discovering relationships between data items within different Linked Data sources."
    )
    .dependsOn(core)

//////////////////////////////////////////////////////////////////////////////
// Dashboard Modules
//////////////////////////////////////////////////////////////////////////////
lazy val dashboard = project
    .settings(commonSettings: _*)
    .dependsOn(core)
    .enablePlugins(PlayScala)

////////////////////////////////////////////////
// root Module
////////////////////////////////////////////////
lazy val root = project.in(file("."))
    .aggregate(core, simulator, dashboard, virtuoso_jena)
    .settings(commonSettings: _*)
    .settings(
      name := "GeoKnow Supply Chain Applications"
    )


//////////////////////////////////////////////////////////////////////////////
// Sub-Modules
//////////////////////////////////////////////////////////////////////////////
lazy val virtuoso_jena = project
