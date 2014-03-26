package dataset

import com.hp.hpl.jena.query.ResultSet

/**
 * Holds a supply chain dataset and allows to query it.
 */
trait Dataset {

  def addresses: Seq[Address]

  def deliveries: Seq[Delivery]

  def query(queryStr: String): ResultSet

  def addListener(listener: Delivery => Unit): Unit = {}

  def contentTypes = {
    deliveries.groupBy(_.content).mapValues(_.size).filter(_._1 != "").toList.sortBy(-_._2)
  }

}
