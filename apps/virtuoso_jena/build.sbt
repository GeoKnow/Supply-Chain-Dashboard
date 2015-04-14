name := "Virtuoso Jena Provider"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies += "org.apache.jena" % "jena-arq" % "2.12.1" excludeAll ExclusionRule(organization = "org.slf4j")

// libraryDependencies += "com.openlink.virtuoso" % "virt_jena2" % "7.1.0"

// libraryDependencies += "com.openlink.virtuoso" % "virtjdbc4" % "7.1.0"

// resolvers += "AKSW repository" at "http://maven.aksw.org/archiva/repository/internal/"