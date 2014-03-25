name := "GeoKnow Supply Chain Applications"

version := "2.6.0-SNAPSHOT"

play.Project.playScalaSettings

lazy val simulator = project

lazy val dashboard = project dependsOn simulator

//lazy val root = project.in(file("."))
//                       .aggregate(dashboard)
//                       .dependsOn(dashboard)
