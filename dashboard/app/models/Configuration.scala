package models

import play.api.Play
import play.api.Play.current
import supplychain.dataset.{EndpointConfig, Endpoint}
import supplychain.model.DateTime

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
                          productUri: String,
                          minStartDate: DateTime,
                          maxEndDate: DateTime,
                          tickIntervalsDays: Double,
                          orderIntervalDays: Double,
                          orderCount: Int)

/**
 * Holds the configuration.
 */
object Configuration {

  /* Loads and holds the configuration. */
  lazy val get = {
    val config = Play.configuration

    Configuration(
      endpointConfig = EndpointConfig(doInit = false,
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
      productUri = config.getString("simulator.product.uri").getOrElse(null),
      minStartDate = DateTime.parse(config.getString("simulator.minStartDate").getOrElse("2014-01-01")),
      maxEndDate = DateTime.parse(config.getString("simulator.maxEndDate").getOrElse("2014-12-31")),
      tickIntervalsDays = config.getDouble("simulator.defaultTickIntervalDays").getOrElse(1.0),
      orderIntervalDays = config.getDouble("simulator.defaultOrderIntervalDays").getOrElse(1.0),
      orderCount = config.getInt("simulator.defaultOrderCount").getOrElse(10)
    )
  }
}