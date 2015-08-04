package supplychain.simulator

import play.api.Play
import play.api.Play.current
import supplychain.dataset.{EndpointConfig, Endpoint}
import supplychain.model.DateTime

/**
 * The Supply Chain Dashboard configuration.
 */
case class Configuration( endpointConfig: EndpointConfig,
                          productUri: String,
                          minStartDate: DateTime,
                          maxEndDate: DateTime)

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
      productUri = config.getString("simulator.product.uri").getOrElse(null),
      minStartDate = DateTime.parse("yyyy-MM-dd", config.getString("simulator.minStartDate").getOrElse("2014-01-01")),
      maxEndDate = DateTime.parse("yyyy-MM-dd", config.getString("simulator.maxEndDate").getOrElse("2014-12-31"))
    )
  }
}