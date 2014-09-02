name := "simulator"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.3"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.3"

libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.3.3"

libraryDependencies += "com.google.code.gson" % "gson" % "2.2"