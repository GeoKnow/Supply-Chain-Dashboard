package models

import play.api.Play.current
import play.api.libs.concurrent.Akka
import supplychain.dataset.{EndpointConfig, Dataset}

/**
 * Holds the current data set.
 */
object CurrentDataset {

  //val simulator = new Simulator(Akka.system, Configuration.get.endpointConfig, Configuration.get.productUri)

  // supplychain.simulator.Scheduler.lastOrderDate

  // Reference to the current data set.
  @volatile
  private var dataset: Dataset = RdfStoreDataset

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