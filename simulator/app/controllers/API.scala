package controllers

import com.sun.imageio.plugins.common.SubImageInputStream
import play.api.Logger
import play.api.mvc.{Action, Controller}
import supplychain.dataset.MetricsDataset
import supplychain.model.DateTime
import supplychain.simulator.{Configuration, Simulator}

/**
 * The REST API.
 */
object API extends Controller {

  def step(start: Option[String]) = Action {
    Logger.info(s"Simulation advanced one 'Tick' from start date '$start'.")
    val s = start.map(DateTime.parse)
    Simulator.step(s)
    Ok("step")
  }

  def run(start: Option[String], end: Option[String], interval: Double) = Action {
    Logger.info(s"Simulation started at '$start' and will run until '$end' with an interval of '$interval' seconds.")

    val s = start.map(DateTime.parse)
    val e = end.map(DateTime.parse)

    Simulator.run(interval, s, e)
    Ok("run")
  }

  def pause() = Action {
    Logger.info(s"Simulation paused.")
    Simulator().pause()
    Ok("pause")
  }

  def status() = Action {
    Logger.info(s"Provide simulation status information.")
    NotImplemented
  }

  def calculateMetrics() = Action {
    Logger.info(s"Calculate performance metrics.")

    val md = new MetricsDataset(Configuration.get.endpointConfig, Configuration.get.endpointConfig.getDefaultGraph())

    var currentDate = Simulator().startDate
    while(currentDate <= Simulator().simulationEndDate) {
      for (s <- Simulator().network.suppliers) {
        val messages = Simulator().messages.filter(_.date <= currentDate).filter(_.connection.source.id == s.id)
        md.addMetricValue(messages, s, currentDate)
      }
      currentDate += Simulator().tickInterval
    }

    Ok("metrics")
  }
}
