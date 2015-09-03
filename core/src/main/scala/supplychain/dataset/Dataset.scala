package supplychain.dataset

import com.hp.hpl.jena.query.ResultSet
import com.hp.hpl.jena.rdf.model.Model
import supplychain.model.{SimulationUpdate, Message, Connection, Supplier}

/**
 * Holds a supply chain dataset and allows to query it.
 */
trait Dataset {

  /** The list of suppliers. */
  def suppliers: Seq[Supplier]

  /** The connections between suppliers. */
  def connections: Seq[Connection]

  /**
   * Messages that have been exchanged between suppliers along a connection.
   * Messages are ordered by date.
   */
  def messages: Seq[Message]

  def query(queryStr: String): ResultSet

  def describe(queryStr: String): Model

  def addListener(listener: SimulationUpdate => Unit): Unit = {}

  def contentTypes = {
    connections.groupBy(_.content.name).mapValues(_.size).filter(_._1 != "").toList.sortBy(-_._2)
  }

}
