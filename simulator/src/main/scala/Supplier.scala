import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import scala.collection.mutable
import scala.util.Random
import scala.concurrent.duration._
import _root_.Supplier._

/**
 * A supplier that builds products from parts that it receives from other suppliers.
 */
class Supplier(suppliers: Map[Product, ActorRef]) extends Actor {

  val log = Logging(context.system, this)

  val productionTime: FiniteDuration = 1.seconds + Random.nextInt(10).seconds

  val storage = new Storage(suppliers.keys.toList)

  var orders = mutable.Queue[Order]()

  def receive = {
    case OrderMsg(product, count: Int) =>
      log.info("Received order for " + product.name)
      orderParts(product, count)
      orders.enqueue(Order(sender(), product, count))
      tryProduce()

    case ShippingMsg(product, count: Int) =>
      log.info("Received shipping of " + product.name)
      storage.put(product, count)
      tryProduce()
  }

  private def orderParts(product: Product, count: Int): Unit = {
    for(part <- product.parts)
      suppliers(part) ! OrderMsg(part, count * part.count)
  }

  private def tryProduce(): Unit = {
    for(order <- orders.headOption) {
      if(storage.take(order.product.parts)) {
        orders.dequeue()
        context.system.scheduler.scheduleOnce(productionTime, order.sender, ShippingMsg(order.product, order.count))
      }
    }
  }
}

object Supplier {

  case class Order(sender: ActorRef, product: Product, count: Int)

  case class OrderMsg(product: Product, count: Int)

  case class ShippingMsg(product: Product, count: Int)
}