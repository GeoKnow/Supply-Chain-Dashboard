package controllers

import java.io.StringWriter

import com.hp.hpl.jena.query.ResultSetFormatter
import models._
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.mvc.{Action, Controller}
import supplychain.dataset.{DatasetStatistics, Namespaces, SchnelleckeDataset}
import supplychain.model.DateTime
import supplychain.simulator.Simulator

import scala.io.Source

/**
 * The REST API.
 */
object API extends Controller {

  def step() = Action {
    Simulator().step()
    Ok
  }

  def run(start: String, end: String, interval: Double) = Action {
    Logger.info(s"Simulation started at $start and will run until $end with an interval of $interval seconds.")
    Simulator().run(DateTime.parse("yyyy-MM-dd", start), DateTime.parse("yyyy-MM-dd", end), interval)
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
    Ok("stop")
  }

}
