name := "Supply Chain Dashboard"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

////////////////////////////////////////////////
// War Packaging
////////////////////////////////////////////////

com.github.play2war.plugin.Play2WarPlugin.play2WarSettings

com.github.play2war.plugin.Play2WarKeys.servletVersion := "3.0"