package simulation

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import scala.collection.mutable
import scala.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import Supplier._

/**
 * A supplier that builds products from parts that it receives from other suppliers.
 */
class Supplier(suppliers: Map[Product, ActorRef], coordinates: Coordinates) extends Actor {

  val productionTime: FiniteDuration = 1.seconds + Random.nextInt(10).seconds

  val delayProbability = Random.nextDouble() * 0.7

  private val storage = new Storage(suppliers.keys.toList)

  private val orders = mutable.Queue[Order]()

  private val log = Logging(context.system, this)

  def receive = {
    case OrderMsg(product, count: Int) =>
      log.info("Received order for " + product.name)
      orderParts(product, count)
      orders.enqueue(Order(sender, product, count))
      tryProduce()

    case ShippingMsg(coords, product, count: Int) =>
      log.info("Received shipping of " + product.name)
      storage.put(product, count)
      Simulation.addShipping(Shipping(coords, coordinates))
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
        // Determine production time
        val time =
          if(Random.nextBoolean())
            productionTime
          else {
            log.info(s"Shipping of ${order.product.name} will be delayed.")
            productionTime * 2
          }
        // Schedule shipping message
        context.system.scheduler.scheduleOnce(time, order.sender, ShippingMsg(coordinates, order.product, order.count))
      }
    }
  }
}

object Supplier {

  case class Order(sender: ActorRef, product: Product, count: Int)

  case class OrderMsg(product: Product, count: Int)

  case class ShippingMsg(sender: Coordinates, product: Product, count: Int)
}