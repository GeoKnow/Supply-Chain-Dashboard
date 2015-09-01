package supplychain.simulator

import play.api.Play
import play.api.Play.current
import supplychain.dataset.{EndpointConfig}
import supplychain.model.DateTime

/**
 * The Supply Chain Dashboard configuration.
 */
case class Configuration( endpointConfig: EndpointConfig,
                          var productUri: String,
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
      endpointConfig = EndpointConfig(doInit = true,
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
      productUri = config.getString("simulator.product.uri").getOrElse(null),
      minStartDate = DateTime.parse(config.getString("simulator.minStartDate").getOrElse("2014-01-01")),
      maxEndDate = DateTime.parse(config.getString("simulator.maxEndDate").getOrElse("2014-12-31")),
      tickIntervalsDays = config.getDouble("simulator.defaultTickIntervalDays").getOrElse(1.0),
      orderIntervalDays = config.getDouble("simulator.defaultOrderIntervalDays").getOrElse(1.0),
      orderCount = config.getInt("simulator.defaultOrderCount").getOrElse(10)
    )
  }
}