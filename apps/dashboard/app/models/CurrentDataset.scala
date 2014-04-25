package models

import dataset.Dataset
import simulation.SimulatorDataset

/**
 * Holds the current data set.
 */
object CurrentDataset {

  // Reference to the current data set.
  @volatile
  private var dataset: Dataset = new SchnelleckeDataset//SimulatorDataset

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