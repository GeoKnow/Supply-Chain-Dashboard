// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// play2war
// resolvers += Resolver.url("bintray-sbt-plugin-releases", url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.7")

// Plugin for generating WAR files.
// addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.3-beta1")
addSbtPlugin("com.github.play2war" % "play2-war-plugin" % "1.3-beta3")
