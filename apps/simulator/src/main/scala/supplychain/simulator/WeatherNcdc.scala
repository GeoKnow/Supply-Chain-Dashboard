package supplychain.simulator

import play.api.libs.json.{Reads, JsResult}
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.Play.current

/**
 * Created by rene on 01.09.14.
 */
object WeatherNcdc {

  def get(uri: String): String = {

    //implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    val holder: WSRequestHolder = WS.url(uri)
    val complexHolder: WSRequestHolder = holder.withHeaders("token" -> "FHZLEZWsBusSZulVeicGwUzkXTCFTBIp")
    val promise = complexHolder.get()
    val body = promise.value.get.toString
    return body
  }
}

case class NcdcLocation(
						id: String,
						elevation: Int,
						name: String,
						elevationUnit: String,
						datacoverage: Double,
						longitude: Double,
						mindate: String,
						latitude: Double,
						maxdate: String)
                         
case class NcdcMetadata(
						limit: Int,
						count: Int,
						offset: Int)

case class NcdcLocationResults(results: List[NcdcLocation], metadata: NcdcMetadata)
