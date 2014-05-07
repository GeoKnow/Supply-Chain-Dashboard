package supplychain.simulation

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import supplychain.metric.AverageProductionTime

/**
 * Provides the main method for running the simulation on the command line.
 */
object Main extends App {
  val simulation = new Simulation()
  val dataset = new SimulatorDataset()
  val metric = new AverageProductionTime()

  simulation.system.scheduler.schedule(10 seconds, 10 seconds) {
    val result = metric(dataset.messages)
    println(s"${metric.dimension}: $result ${metric.unit}")
  }

}
