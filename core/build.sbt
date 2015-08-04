name := "core"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies += "org.apache.jena" % "jena-core" % "2.11.2" excludeAll ExclusionRule(organization = "org.slf4j")

libraryDependencies += "org.apache.jena" % "jena-arq" % "2.11.2" excludeAll ExclusionRule(organization = "org.slf4j")


libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.3.7"

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
