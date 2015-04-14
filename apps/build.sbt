name := "GeoKnow Supply Chain Applications"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

lazy val virtuoso_jena = project in file("virtuoso_jena")

lazy val core = project dependsOn virtuoso_jena

lazy val simulator = project dependsOn core

lazy val dashboard = project dependsOn core dependsOn simulator enablePlugins PlayScala

lazy val root = project.in(file(".")) aggregate (core, simulator, dashboard, virtuoso_jena)

