package models

import supplychain.dataset.Dataset
import supplychain.simulation.{Simulation, SimulatorDataset}

/**
 * Holds the current data set.
 */
object CurrentDataset {

  // Reference to the current data set.
  @volatile
  private var dataset: Dataset = new SimulatorDataset()

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