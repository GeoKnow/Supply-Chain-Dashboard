package models

import play.api.Play
import play.api.Play.current

/**
 * The Supply Chain Dashboard configuration.
 *
 * @param silkUrl The base URL of the Silk installation.
 * @param silkProject The project in the Silk Workbench that holds the metrics.
 */
case class Configuration(
                          endpointUrl: String,
                          virtuosoHost: String,
                          virtuosoPort: String,
                          virtuosoUser: String,
                          virtuosoPassword: String,
                          endpointType: String,
                          defaultGraph: String,
                          silkUrl: String,
                          silkProject: String,
                          silkTask: String,
                          defaultGraphWeather: String,
                          defaultGraphConfiguration: String,
                          productUri: String)

/**
 * Holds the configuration.
 */
object Configuration {

  /* Loads and holds the configuration. */
  lazy val get = {
    val config = Play.configuration

    Configuration(
      endpointUrl = config.getString("simulator.endpoint.url").getOrElse(""),
      virtuosoHost = config.getString("simulator.virtuoso.host").getOrElse("localhost"),
      virtuosoPort = config.getString("simulator.virtuoso.port").getOrElse("1111"),
      virtuosoUser = config.getString("simulator.virtuoso.user").getOrElse("dba"),
      virtuosoPassword = config.getString("simulator.virtuoso.password").getOrElse("dba"),
      endpointType = config.getString("simulator.endpoint.kind").getOrElse("local"),
      defaultGraph = config.getString("simulator.defaultGraph").getOrElse("http://xybermotive.com/geoknow/"),
      silkUrl = config.getString("dashboard.silkUrl").getOrElse("http://localhost:9000/"),
      silkProject = config.getString("dashboard.silkProject").getOrElse("supplychainmetrics"),
      silkTask = config.getString("dashboard.silkTask").getOrElse("metrics"),
      defaultGraphWeather = config.getString("simulator.defaultGraphWeather").getOrElse("http://www.xybermotive.com/GeoKnowWeather#"),
      defaultGraphConfiguration = config.getString("simulator.defaultGraphConfiguration").getOrElse("http://www.xybermotive.com/configuration/"),
      productUri = config.getString("simulator.product.uri").getOrElse(null)
    )
  }
}