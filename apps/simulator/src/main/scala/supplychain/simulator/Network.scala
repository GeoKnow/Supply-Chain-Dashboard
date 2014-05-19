package supplychain.simulator

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

class Network(val actor: ActorRef, val product: Product, val suppliers: Seq[Supplier], val connections: Seq[Connection]) {

  // Create an OEM actor that is responsible for sending the initial orders for the product
  private val oemSupplier = Network.generateSupplier(Product("OEM", parts = product :: Nil))
  Network.createActor(oemSupplier)

  def order() {
    // Create a new order with a dummy connection
    val order =
      Order(
        date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat,
        connection = Connection("Initial", product, suppliers.head, oemSupplier),
        count = 1
      )
    actor ! order
  }
}

object Network {

  private val random = new Random(0)

  def build(product: Product): Network = {
    val suppliers = for(part <- product :: product.partList) yield generateSupplier(part)
    val connections = new SimpleNetworkBuilder(suppliers).apply(product)
    val actors = for(supplier <- suppliers) yield createActor(supplier)

    new Network(actors.head, product, suppliers, connections)
  }

  def createActor(supplier: Supplier): ActorRef = {
    Simulator.system.actorOf(Props(classOf[SupplierActor], supplier), supplier.uri)
  }

  def generateSupplier(product: Product) = {
    val lat = 47 + 7 * random.nextDouble()
    val lon = 6 + 9 * random.nextDouble()
    val coordinates = Coordinates(lat, lon)
    val address = Address("Beispielstr. 1", "10123", "Musterstadt", "Deutschland")
    Supplier(
      product.name + "-" + UUID.randomUUID.toString,
      product.name + " Supplier", address, coordinates, product)
  }
}
