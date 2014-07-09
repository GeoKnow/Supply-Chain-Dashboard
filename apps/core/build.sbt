name := "core"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies += "silk-workspace" %% "silk-workspace" % "2.6.1-SNAPSHOT"

libraryDependencies += "org.apache.jena" % "jena-core" % "2.11.2" excludeAll ExclusionRule(organization = "org.slf4j")

libraryDependencies += "org.apache.jena" % "jena-arq" % "2.11.2" excludeAll ExclusionRule(organization = "org.slf4j")
