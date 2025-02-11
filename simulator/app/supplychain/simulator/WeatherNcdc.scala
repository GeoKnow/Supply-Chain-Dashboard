package supplychain.simulator

import java.util.logging.Logger

import play.api.libs.json.{JsValue, Reads, JsResult}
import play.api.libs.ws._
import supplychain.dataset.WeatherProvider
import supplychain.model.DateTime
import WeatherProvider._
import scala.concurrent.Future
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/**
 * Created by rene on 01.09.14.
 */
object WeatherNcdc {

  private val log = Logger.getLogger(getClass.getName)

  private val tokens = List(//"grPjwwJDoWFSTtwDLxjZGoPBZNMVJthw",
                            //"FHZLEZWsBusSZulVeicGwUzkXTCFTBIp",
                            //"IzjHnCVFZDKHggEMUuHFbQUfDBSiiwHY",
                            "iHuyuLYggIIcmaLqzcrnjnJZcBvUjtWV",
                            "yUesEAjpGjUqeAAYYaHlctfGhbFtHpkT",
                            "VFXjZbwWUijCSxMZrPJdczhGhOOWfNoT",
                            "UjgFLEdUegHNIEfGKeECMsxTbjTKXbHy",
                            "gZaRadlsQFNzIwSPQAhLutWoSrmrqpYB")

  private var nextRequest = DateTime.now

  def get(uri: String): Future[String] = {
    log.info("uri: " + uri)
    val holder = WS.url(uri)
    val tokenIndex = Random.nextInt(tokens.size)
    val complexHolder = holder.withHeaders("token" -> tokens(tokenIndex))
    val promise = complexHolder.get()

    for (response <- promise) yield {
      if (response.status != 200) {
        log.info(response.status.toString)
        log.info(response.body)
      }

      if (response.status == 429) {
        //log.info("sleeping for 5s")
        log.info("token:" + tokens(tokenIndex))
        //Thread sleep 5000
        return get(uri)
      }

      response.body
    }
  }
}

case class NcdcLocationResults(results: List[NcdcLocation], metadata: NcdcMetadata)

case class NcdcLocation(  id: String,
                          elevation: Int,
                          name: String,
                          elevationUnit: String,
                          datacoverage: Double,
                          longitude: Double,
                          mindate: String,
                          latitude: Double,
                          maxdate: String)



case class NcdcDailySummaryResult(results: List[NcdcDailySummary], metadata: NcdcMetadata)

case class NcdcDailySummary(  station: String,
                              value: Int,
                              attributes: String,
                              datatype: String,
                              date: String)



case class NcdcMetadata(resultset: NcdcResultset)

case class NcdcResultset (limit: Int, count: Int, offset: Int) {
  override def toString: String =
    "limit: " + limit.toString + "; count: " + count.toString + "; offset: " + offset.toString
}