package supplychain.model

import play.api.libs.json.{Json, Writes}

/**
 * Created by rene on 03.09.15.
 */
case class SimulationUpdate(currentDate: DateTime, messages: Seq[Message]) {

}

object SimulationUpdate {
  implicit val simulationUpdateWrites = new Writes[SimulationUpdate] {
    def writes(su: SimulationUpdate) = Json.obj(
      "currentDate" -> su.currentDate.toYyyyMMdd(),
      "orders" -> Json.toJson(su.messages.collect{case o:Order => o}),
      "shippings" -> Json.toJson(su.messages.collect{case s:Shipping => s})
    )
  }
}