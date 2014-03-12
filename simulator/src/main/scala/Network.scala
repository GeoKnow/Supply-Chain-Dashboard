import _root_.Supplier.{OrderMsg, ShippingMsg}
import akka.actor.{Actor, ActorSystem, Props, ActorRef}
import akka.event.Logging
import scala.concurrent.duration._

object Network {

  private val system = ActorSystem("system")

  def build(product: Product) = {
    val oem = buildNetwork(product)
    system.actorOf(Props(classOf[Consumer], product, oem), "Consumer")
  }

  private def buildNetwork(product: Product): ActorRef = {
    val suppliers = for(part <- product.parts) yield buildNetwork(part)
    system.actorOf(Props(classOf[Supplier], (product.parts zip suppliers).toMap), product.name + "_Supplier")
  }

  private class Consumer(product: Product, supplier: ActorRef) extends Actor {

    val log = Logging(context.system, this)

    override def preStart() {
      context.system.scheduler.schedule(1 seconds, 60 seconds, supplier, OrderMsg(product, 1))
    }

    def receive = {
      case ShippingMsg(shippedProduct, count) =>
        log.info(s"Produced $count ${shippedProduct.name}(s)")
    }
  }
}
