package models

import com.hp.hpl.jena.query.ResultSet

trait Dataset {

  def addresses: Seq[Address]

  def deliveries: Seq[Delivery]

  def query(queryStr: String): ResultSet
}

/**
 * Holds the supply chain dataset and allows to query it.
 */
object Dataset {

  @volatile
  private var current: Dataset = new SchnelleckeDataset()

  /**
   * Gets the current dataset.
   */
  def apply(): Dataset = current

  /**
   * Updates the current dateset.
   */
  def update(dataset: Dataset) {
    current = dataset
  }
}
