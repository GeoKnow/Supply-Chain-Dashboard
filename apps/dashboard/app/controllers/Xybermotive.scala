package controllers

import models.CurrentDataset
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import supplychain.dataset.DatasetStatistics
import supplychain.metric.{Evaluator, Metrics}
import supplychain.model.{Order, Shipping, _}

object Xybermotive extends Controller {

  def inventory(supplierId: String) = Action {
    val xyData = XybermotiveData.retrieve(supplierId)
    val dateString = DateTime.now.toFormat("dd.MM.yyyy hh:mm:ss")
    Ok(views.html.xybermotive.inventory(xyData, dateString, supplierId))
  }

  def inventoryJson(supplierId: String) = Action {
    Ok(XybermotiveData.retrieveJson(supplierId))
  }

  def alternativeSupplierJson(sid: String, pnr: String) = Action {
    val allInventories = XybermotiveData.getAllInventory(CurrentDataset().suppliers)
    val filteredInventory = allInventories.map(_.filter(pnr)).filter(_.data.nonEmpty)
    Ok(Json.toJson(filteredInventory))
  }

  def alternativeSupplier(sid: String, pnr: String) = Action {
    val allInventories = XybermotiveData.getAllInventory(CurrentDataset().suppliers)
    val filteredInventory = allInventories.map(_.filter(pnr)).filter(_.data.nonEmpty)
    Ok(views.html.xybermotive.alternativeSuppliers(filteredInventory))
  }

}