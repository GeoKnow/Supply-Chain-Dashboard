package models

import com.hp.hpl.jena.query.ResultSet

/**
 * Holds a supply chain dataset and allows to query it.
 */
trait Dataset {

  def addresses: Seq[Address]

  def deliveries: Seq[Delivery]

  def query(queryStr: String): ResultSet

  def contentTypes = {
    deliveries.groupBy(_.content).mapValues(_.size).filter(_._1 != "").toList.sortBy(-_._2)
  }

}

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
