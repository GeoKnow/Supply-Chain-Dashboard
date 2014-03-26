name := "GeoKnow Supply Chain Applications"

version := "2.6.0-SNAPSHOT"

play.Project.playScalaSettings

lazy val core = project

lazy val simulator = project dependsOn core

lazy val dashboard = project dependsOn core dependsOn simulator

lazy val root = project.in(file("."))
                .aggregate(dashboard)
                .dependsOn(dashboard)
