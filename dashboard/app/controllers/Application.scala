package controllers

import models._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{JsPath, Writes, JsValue, Json}
import play.api.mvc._
import supplychain.dataset.DatasetStatistics
import supplychain.metric.{Evaluator, Metrics}
import supplychain.model.{Order, Shipping, _}

object Application extends Controller {

  def print() {
  }

  def query = Action {
    Ok(views.html.query())
  }

  def map = Action {
    Ok(views.html.map())
  }

  def metrics(supplierId: String) = Action {
    val messages = CurrentDataset().messages.filter(_.connection.source.id == supplierId)
    val supplier = CurrentDataset().suppliers.find(_.id == supplierId).get
    Ok(views.html.metrics(messages, supplier))
  }

  def news(supplierId: String) = Action {
    val ec = Configuration.get.endpointConfig
    val np = new NewsProvider(ec)
    val supplier = CurrentDataset().suppliers.find(_.id == supplierId)
    if (supplier.isDefined) {
      val date = RdfStoreDataset.Scheduler.currentDate
      val news = np.getNews(supplier.get, date)
      Ok(views.html.news(news, supplier.get))
    } else {
      NotFound
    }
  }

  def report(supplierId: String) = Action {
    val scoreTable = Evaluator.table(CurrentDataset(), Metrics.all, supplierId)
    Ok(views.html.report(scoreTable))
  }

  def deliveryStream = Action {

    val stats = new DatasetStatistics(CurrentDataset())

    def addDueParts(json: JsValue): JsValue = {
      json match {
        case obj: JsObject => {
          val duePartsSeq = for (s <- CurrentDataset().suppliers) yield {
            val dueParts = stats.dueParts(s)
            s.id -> JsNumber(dueParts)
          }
          obj + ("dueParts", JsObject(duePartsSeq))
        }
      }
    }

    val (simulationUpdateEnumerator, simulationUpdateChannel) = Concurrent.broadcast[JsValue]
    val listener = (msg: SimulationUpdate) => simulationUpdateChannel.push(addDueParts(Json.toJson(msg)))

    CurrentDataset().addListener(listener)

    val simuzlationUpdateEnumeratee = simulationUpdateEnumerator &> Comet(callback = "parent.myPostMessage")

    Ok.chunked(simuzlationUpdateEnumeratee)
  }

  def messages(supplierId: String) = Action {
    val downstreamMessages = CurrentDataset().messages.filter(_.connection.source.id == supplierId)
    val upstreamMessages = CurrentDataset().messages.filter(_.connection.target.id == supplierId)

    Ok(views.html.messages(downstreamMessages, upstreamMessages))
  }

}