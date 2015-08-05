package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}
import supplychain.model.DateTime
import supplychain.simulator.Simulator

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
}
