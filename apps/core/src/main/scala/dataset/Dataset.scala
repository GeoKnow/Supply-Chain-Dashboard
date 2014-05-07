package dataset

import com.hp.hpl.jena.query.ResultSet
import com.hp.hpl.jena.rdf.model.Model

/**
 * Holds a supply chain dataset and allows to query it.
 */
trait Dataset {

  def suppliers: Seq[Supplier]

  def deliveries: Seq[Connection]

  def query(queryStr: String): ResultSet

  def describe(queryStr: String): Model = ???

  def addListener(listener: Message => Unit): Unit = {}

  def contentTypes = {
    deliveries.groupBy(_.content.name).mapValues(_.size).filter(_._1 != "").toList.sortBy(-_._2)
  }

}
