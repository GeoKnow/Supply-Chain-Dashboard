package models

import play.api.Play
import play.api.Play.current

/**
 * The Supply Chain Dashboard configuration.
 *
 * @param silkUrl The base URL of the Silk installation.
 * @param silkProject The project in the Silk Workbench that holds the metrics.
 */
case class Configuration(endpointUrl: String, defaultGraph: String, silkUrl: String, silkProject: String, silkTask: String, defaultGraphWeather: String)

/**
 * Holds the configuration.
 */
object Configuration {

  /* Loads and holds the configuration. */
  lazy val get = {
    val config = Play.configuration

    Configuration(
      endpointUrl = config.getString("simulator.endpoint").getOrElse(""),
      defaultGraph = config.getString("simulator.defaultGraph").getOrElse("http://xybermotive.com/geoknow"),
      silkUrl = config.getString("dashboard.silkUrl").getOrElse("http://localhost:9000/"),
      silkProject = config.getString("dashboard.silkProject").getOrElse("supplychainmetrics"),
      silkTask = config.getString("dashboard.silkTask").getOrElse("metrics"),
      defaultGraphWeather = config.getString("simulator.defaultGraphWeather").getOrElse("http://www.xybermotive.com/GeoKnowWeather#")
    )
  }
}