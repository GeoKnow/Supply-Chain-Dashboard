package supplychain.simulation

import scala.util.Random
import akka.actor.{Props, ActorRef}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import supplychain.model._
import java.util.{GregorianCalendar, UUID}
import javax.xml.datatype.DatatypeFactory
import supplychain.model.Connection
import supplychain.model.Coordinates
import supplychain.model.Supplier
import supplychain.model.Address
import supplychain.model.Product

class Network(val actor: ActorRef, val suppliers: Seq[Supplier], val connections: Seq[Connection]) {

  def start(simulation: Simulation) {
    val order =
      Order(
        date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat,
        connection = simulation.connections.head,
        count = 1
      )
    simulation.system.scheduler.schedule(1 seconds, 30 seconds, actor, order)
  }
}

object Network {

  val random = new Random(0)

  def build(product: Product, simulation: Simulation): Network = {
    val endProduct = Product("End_Product", 1, product :: Nil)
    val suppliers = for(part <- endProduct :: endProduct.partList) yield generateSupplier(part)
    val connections = new SimpleNetworkBuilder(suppliers).apply(endProduct)
    val actors = for(supplier <- suppliers) yield createActor(supplier, simulation)

    new Network(actors.head, suppliers, connections)
  }

  def createActor(supplier: Supplier, simulation: Simulation): ActorRef = {
    simulation.system.actorOf(Props(classOf[SupplierActor], supplier, simulation), supplier.uri)
  }

  def generateSupplier(product: Product) = {
    val lat = 47 + 7 * random.nextDouble()
    val lon = 6 + 9 * random.nextDouble()
    val coordinates = Coordinates(lat, lon)
    val address = Address("", "", "", "")
    Supplier(
      product.name + "-" + UUID.randomUUID.toString,
      product.name + " Supplier", address, coordinates, product)
  }
}
