name := "GeoKnow Supply Chain Applications"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

lazy val core = project

lazy val simulator = project dependsOn core

lazy val dashboard = project dependsOn core dependsOn simulator enablePlugins PlayScala

lazy val root = project.in(file(".")) aggregate (core, simulator, dashboard)
