package simulation


import scala.util.Random
import SupplierActor.{OrderMsg, ShippingMsg}
import akka.actor.{Actor, ActorSystem, Props, ActorRef}
import akka.event.Logging
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import dataset._
import java.util.UUID
import simulation.SupplierActor.OrderMsg
import dataset.Connection
import dataset.Coordinates
import dataset.Supplier
import dataset.Address

class Network(val actor: ActorRef, val suppliers: Seq[Supplier], val connections: Seq[Connection]) {

  def start(simulation: Simulation) {
    simulation.system.scheduler.schedule(1 seconds, 60 seconds, actor, OrderMsg(1))
  }
}

object Network {

  val random = new Random(0)

  def build(product: dataset.Product, simulation: Simulation): Network = {
    val endProduct = Product("End Product", 1, product :: Nil)
    val suppliers = for(part <- endProduct :: endProduct.partList) yield generateSupplier(part)
    val connections = new SimpleNetworkBuilder(suppliers).apply(endProduct)
    val actors = for(supplier <- suppliers) yield createActor(supplier, simulation)

    new Network(actors.head, suppliers, connections)
  }

  def createActor(supplier: Supplier, simulation: Simulation): ActorRef = {
    simulation.system.actorOf(Props(classOf[SupplierActor], supplier, simulation), supplier.uri)
  }

  def generateSupplier(product: dataset.Product) = {
    val lat = 47 + 7 * random.nextDouble()
    val lon = 6 + 9 * random.nextDouble()
    val coordinates = Coordinates(lat, lon)
    val address = Address("", "", "", "")
    Supplier(UUID.randomUUID.toString, product.name + " Supplier", address, coordinates, product)
  }
}
