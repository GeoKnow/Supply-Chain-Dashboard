package supplychain.metric

object Metrics {
  val all = new AverageProductionTime() :: new DueOrders() :: Nil
}
