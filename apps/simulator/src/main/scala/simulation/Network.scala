package simulation


import scala.util.Random
import Supplier.{OrderMsg, ShippingMsg}
import akka.actor.{Actor, ActorSystem, Props, ActorRef}
import akka.event.Logging
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class Network(actor: ActorRef) {

}

object Network {

  private val system = ActorSystem("system")

  def build(product: Product): Network = {
    val supplier = buildNetwork(product)
    val consumer = system.actorOf(Props(classOf[Consumer], product, supplier), "Consumer")
    new Network(consumer)
  }

  def buildNetwork(product: Product): ActorRef = {
    val suppliers = for(part <- product.parts) yield buildNetwork(part)
    val supplierMap = (product.parts zip suppliers).toMap
    val coordinates = generateCoordinates()
    system.actorOf(Props(classOf[Supplier], supplierMap, coordinates), product.name + "_Supplier")
  }

  def generateCoordinates() = {
    val lat = 52.5 + 0.1 * Random.nextDouble()
    val lon = 9.65 + 0.2 * Random.nextDouble()
    Coordinates(lat, lon)
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
