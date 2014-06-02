package models

import supplychain.dataset.Dataset
import supplychain.simulator.{Simulation, Simulator}
import play.api.libs.concurrent.Akka
import play.api.Play.current

/**
 * Holds the current data set.
 */
object CurrentDataset {

  val simulator = new Simulator(Akka.system)

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