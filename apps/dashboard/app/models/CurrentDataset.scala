package models

import play.api.Play.current
import play.api.libs.concurrent.Akka
import supplychain.dataset.{EndpointConfig, Dataset}
import supplychain.simulator.Simulator

/**
 * Holds the current data set.
 */
object CurrentDataset {

  val endpointConfig = new EndpointConfig(
    Configuration.get.endpointType,
    Configuration.get.defaultGraph,
    Configuration.get.defaultGraphWeather,
    Configuration.get.endpointUrl,
    Configuration.get.virtuosoHost,
    Configuration.get.virtuosoPort,
    Configuration.get.virtuosoUser,
    Configuration.get.virtuosoPassword)

  val simulator = new Simulator(Akka.system, endpointConfig)

  // supplychain.simulator.Scheduler.lastOrderDate

  // Reference to the current data set.
  @volatile
  private var dataset: Dataset = simulator

  /**
   * Gets the current data set.
   */
  def apply(): Dataset = dataset

  /**
   * Updates the current date set.
   */
  def update(newDataset: Dataset) {
    dataset = newDataset
  }
}