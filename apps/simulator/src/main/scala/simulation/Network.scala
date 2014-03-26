package simulation


import scala.util.Random
import Supplier.{OrderMsg, ShippingMsg}
import akka.actor.{Actor, ActorSystem, Props, ActorRef}
import akka.event.Logging
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import dataset.Address
import java.util.UUID

class Network(actor: ActorRef, val suppliers: Seq[Address]) {

}

object Network {

  private val system = ActorSystem("system")

  def build(product: Product, simulation: Simulation): Network = {
    val (supplier, addresses) = buildNetwork(product, simulation)
    val consumer = system.actorOf(Props(classOf[Consumer], product, supplier), "Consumer")
    new Network(consumer, addresses)
  }

  def buildNetwork(product: Product, simulation: Simulation): (ActorRef, Seq[Address]) = {
    val suppliers = for(part <- product.parts) yield buildNetwork(part, simulation)
    val supplierMap = (product.parts zip suppliers.map(_._1)).toMap
    val coordinates = generateAddress(product)
    val actor = system.actorOf(Props(classOf[Supplier], supplierMap, coordinates, simulation), product.name + "_Supplier")
    (actor, suppliers.flatMap(_._2) :+ coordinates)
  }

  def generateAddress(product: Product) = {
    val lat = 47 + 7 * Random.nextDouble()
    val lon = 6 + 9 * Random.nextDouble()
    Address(UUID.randomUUID.toString, product.name + " Supplier", "", "", "", "", lat, lon)
  }

  private class Consumer(product: Product, supplier: ActorRef) extends Actor {

    val log = Logging(context.system, this)

    override def preStart() {
      context.system.scheduler.schedule(1 seconds, 60 seconds, supplier, OrderMsg(product, 1))
    }

    def receive = {
      case ShippingMsg(coords, shippedProduct, count) =>
        log.info(s"Produced $count ${shippedProduct.name}(s)")
    }
  }
}
