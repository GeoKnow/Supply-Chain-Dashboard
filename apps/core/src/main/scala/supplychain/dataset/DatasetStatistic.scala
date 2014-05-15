package supplychain.dataset

import supplychain.model.{Message, Shipping, Order, Connection}

class DatasetStatistic(dataset: Dataset) {

  private var dueOrders: Map[Connection, Int] = dataset.connections.map(c => (c, 0)).toMap

  private def processMessage(msg: Message) = msg match {
    case o: Order => dueOrders = dueOrders.updated(msg.connection , dueOrders(msg.connection) + o.count)
    case sh: Shipping => dueOrders = dueOrders.updated(msg.connection , dueOrders(msg.connection) - sh.count)
  }

}