package models

import play.api.Play
import play.api.Play.current

/**
 * The Supply Chain Dashboard configuration.
 *
 * @param silkUrl The base URL of the Silk installation.
 * @param silkProject The project in the Silk Workbench that holds the metrics.
 */
case class Configuration(silkUrl: String, silkProject: String)

/**
 * Holds the configuration.
 */
object Configuration {

  /* Loads and holds the configuration. */
  lazy val get = {
    val config = Play.configuration

    Configuration(
      silkUrl = config.getString("dashboard.silkUrl").getOrElse("http://localhost:9000/"),
      silkProject = config.getString("dashboard.silkProject").getOrElse("supplychainmetrics")
    )
  }
}