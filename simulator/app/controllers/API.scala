package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}
import supplychain.model.DateTime
import supplychain.simulator.Simulator

/**
 * The REST API.
 */
object API extends Controller {

  def step(start: String) = Action {
    Logger.info(s"Simulation advanced one 'Tick' from start date $start.")
    var s: DateTime = null
    if (start != "default") s = DateTime.parse(start)
    Simulator().step(s)
    Ok("step")
  }

  def run(start: String, end: String, interval: Double) = Action {
    Logger.info(s"Simulation started at $start and will run until $end with an interval of $interval seconds.")
    var s: DateTime = null
    if (start != "default") s = DateTime.parse(start)
    var e: DateTime = null
    if (end != "default") e = DateTime.parse(end)
    Simulator().run(interval, s, e)
    Ok("run")
  }

  def start(interval: Double) = Action {
    Logger.info(s"Simulation (re)started with an interval of $interval seconds.")
    Simulator().start(interval)
    Ok("start")
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
