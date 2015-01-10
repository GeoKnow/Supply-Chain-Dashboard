package models

import play.api.Play.current
import play.api.libs.concurrent.Akka
import supplychain.dataset.Dataset
import supplychain.simulator.Simulator

/**
 * Holds the current data set.
 */
object CurrentDataset {

  val simulator = new Simulator(Akka.system, Configuration.get.endpointUrl, Configuration.get.defaultGraph, Configuration.get.defaultGraphWeather)

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