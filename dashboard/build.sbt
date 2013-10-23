name := "dashboard"

version := "1.0-SNAPSHOT"

libraryDependencies += "org.apache.jena" % "jena-core" % "2.11.0" excludeAll(ExclusionRule(organization = "org.slf4j"))

libraryDependencies += "org.apache.jena" % "jena-arq" % "2.11.0" excludeAll(ExclusionRule(organization = "org.slf4j"))

play.Project.playScalaSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings