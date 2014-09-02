package supplychain.simulator

import play.api.libs.json.{JsValue, Reads, JsResult}
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rene on 01.09.14.
 */
object WeatherNcdc {

  def get(uri: String): Future[JsValue] = {

    val holder = WS.url(uri)
    val complexHolder = holder.withHeaders("token" -> "FHZLEZWsBusSZulVeicGwUzkXTCFTBIp")
    val promise = complexHolder.get()

    for (response <- promise) yield {
      response.json
    }
  }
}


case class NcdcLocation(  id: String,
                          elevation: Int,
                          name: String,
                          elevationUnit: String,
                          datacoverage: Double,
                          longitude: Double,
                          mindate: String,
                          latitude: Double,
                          maxdate: String)
                         
case class NcdcMetadata(resultset: NcdcResultset)

case class NcdcResultset (limit: Int, count: Int, offset: Int)

case class NcdcLocationResults(results: List[NcdcLocation], metadata: NcdcMetadata)



case class NcdcDailySummary(  station: String,
                              value: Int,
                              attributes: String,
                              datatype: String,
                              date: String)

case class NcdcDailySummaryResult(results: List[NcdcDailySummary], metadata: NcdcMetadata)