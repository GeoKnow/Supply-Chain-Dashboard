package models

import play.api.Play
import play.api.Play.current
import supplychain.dataset.{EndpointConfig, Endpoint}

/**
 * The Supply Chain Dashboard configuration.
 *
 * @param silkUrl The base URL of the Silk installation.
 * @param silkProject The project in the Silk Workbench that holds the metrics.
 */
case class Configuration( endpointConfig: EndpointConfig,
                          silkUrl: String,
                          silkProject: String,
                          silkTask: String,
                          productUri: String)

/**
 * Holds the configuration.
 */
object Configuration {

  /* Loads and holds the configuration. */
  lazy val get = {
    val config = Play.configuration

    Configuration(
      endpointConfig = EndpointConfig(
        kind = config.getString("simulator.endpoint.kind").getOrElse("local"),
        defaultGraph = config.getString("simulator.defaultGraph").getOrElse("http://xybermotive.com/geoknow/"),
        defaultGraphWeather = config.getString("simulator.defaultGraphWeather").getOrElse("http://www.xybermotive.com/GeoKnowWeather#"),
        defaultGraphConfiguration = config.getString("simulator.defaultGraphConfiguration").getOrElse("http://www.xybermotive.com/configuration/"),
        url = config.getString("simulator.endpoint.url").getOrElse(""),
        host = config.getString("simulator.virtuoso.host").getOrElse("localhost"),
        port = config.getString("simulator.virtuoso.port").getOrElse("1111"),
        user = config.getString("simulator.virtuoso.user").getOrElse("dba"),
        password = config.getString("simulator.virtuoso.password").getOrElse("dba")
      ),
      silkUrl = config.getString("dashboard.silkUrl").getOrElse("http://localhost:9000/"),
      silkProject = config.getString("dashboard.silkProject").getOrElse("supplychainmetrics"),
      silkTask = config.getString("dashboard.silkTask").getOrElse("metrics"),
      productUri = config.getString("simulator.product.uri").getOrElse(null)
    )
  }
}