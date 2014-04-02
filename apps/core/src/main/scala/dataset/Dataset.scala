package dataset

import com.hp.hpl.jena.query.ResultSet

/**
 * Holds a supply chain dataset and allows to query it.
 */
trait Dataset {

  def suppliers: Seq[Supplier]

  def deliveries: Seq[Connection]

  def query(queryStr: String): ResultSet

  def addListener(listener: Shipping => Unit): Unit = {}

  def contentTypes = {
    deliveries.groupBy(_.content).mapValues(_.size).filter(_._1 != "").toList.sortBy(-_._2)
  }

}
