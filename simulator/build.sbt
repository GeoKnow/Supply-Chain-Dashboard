name := "simulator"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.4"

libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.3.7"

libraryDependencies += "com.typesafe.play" % "play-ws_2.11" % "2.3.7"

// libraryDependencies += "com.google.code.gson" % "gson" % "2.2"