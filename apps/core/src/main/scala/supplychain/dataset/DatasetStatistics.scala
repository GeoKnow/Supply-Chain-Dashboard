package supplychain.dataset

import supplychain.model._
import supplychain.model.Connection
import supplychain.model.Order
import supplychain.model.Shipping

class DatasetStatistics(dataset: Dataset) {

  //private var dueOrders: Map[Connection, Int] = dataset.connections.map(c => (c, 0)).toMap

  /** Computes the number of due parts (i.e., parts that have been ordered but not delivered yet) */
  def dueParts(supplier: Supplier) = {
    var due = 0
    for(msg <- dataset.messages.filter(_.connection.source.id == supplier.id)) msg match {
      case o: Order => due += o.count
      case s: Shipping => due -= s.count
    }
    due
  }

  /** Computes the number of due parts (i.e., parts that have been ordered but not delivered yet) */
  def dueParts(connection: Connection) = {
    var due = 0
    for(msg <- dataset.messages.filter(_.connection.id == connection.id)) msg match {
      case o: Order => due += o.count
      case s: Shipping => due -= s.count
    }
    due
  }


  //TODO data set statistics should be updated automatically
//  private def processMessage(msg: Message) = msg match {
//    case o: Order => dueOrders = dueOrders.updated(msg.connection , dueOrders(msg.connection) + o.count)
//    case sh: Shipping => dueOrders = dueOrders.updated(msg.connection , dueOrders(msg.connection) - sh.count)
//  }

}