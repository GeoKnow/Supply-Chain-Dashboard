package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}
import supplychain.dataset.MetricsDataset
import supplychain.model.DateTime
import supplychain.simulator.exceptions.SimulationPeriodOutOfBoundsException
import supplychain.simulator.{Configuration, Simulator}
import supplychain.exceptions.UnknownProductException

/**
 * The REST API.
 */
object API extends Controller {

  def step(start: Option[String], productUri: Option[String], graphUri: Option[String]) = Action {
    Logger.info(s"Simulation advanced one 'Tick' from start date '$start'.")
    val s = start.map(DateTime.parse)

    try {
      Simulator.step(s, productUri, graphUri)
      Ok("step")
    } catch {
      case e1: UnknownProductException => BadRequest(e1.message)
      case e2: SimulationPeriodOutOfBoundsException => BadRequest(e2.message)
      case e3: Exception => BadRequest(e3.getMessage)
    }
  }

  def run(start: Option[String], end: Option[String], productUri: Option[String], graphUri: Option[String], interval: Double) = Action {
    Logger.info(s"Simulation started at '$start' and will run until '$end' with an interval of '$interval' seconds.")

    val s = start.map(DateTime.parse)
    val e = end.map(DateTime.parse)

    try {
      Simulator.run(interval, s, e, productUri, graphUri)
      Ok("run")
    } catch {
      case e1: UnknownProductException => BadRequest(e1.message)
      case e2: SimulationPeriodOutOfBoundsException => BadRequest(e2.message)
      case e3: Exception => BadRequest(e3.getMessage)
    }
  }

  def pause() = Action {
    Logger.info(s"Simulation paused.")
    Simulator().pause()
    Ok("pause")
  }

  def calculateMetrics(productUri: Option[String], graphUri: Option[String]) = Action {
    if (!Simulator.isSimulationRunning()) {
      Logger.info(s"Calculate performance metrics.")

      for (p <- productUri) Configuration.get.productUri = p
      for (g <- graphUri) Configuration.get.endpointConfig.defaultGraph = g

      val md = new MetricsDataset(Configuration.get.endpointConfig)

      md.generateDataSet()

      var currentDate = Simulator().startDate
      while(currentDate <= Simulator().simulationEndDate) {
        for (s <- Simulator().network.suppliers) {
          val messages = Simulator().messages.filter(_.date <= currentDate).filter(_.connection.source.id == s.id)
          md.addMetricValue(messages, s, currentDate)
        }
        currentDate += Simulator().tickInterval
      }

      md.normalizeDataCube()

      Ok("metrics")
    } else {
      Status(503)("Simulation is running, can not calculate performance metrics now. Retry later.")
    }
  }
}