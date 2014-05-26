package supplychain.simulator

import scala.util.Random
import akka.actor.{ActorSystem, Cancellable, Props, ActorRef}
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

class Network(simulator: Simulator, val actor: ActorRef, val product: Product, val suppliers: Seq[Supplier], val connections: Seq[Connection]) {

  // Create an OEM actor that is responsible for sending the initial orders for the product
  private val oemSupplier = Network.generateSupplier(Product("OEM", parts = product :: Nil))
  Network.createActor(oemSupplier)(simulator)

  private var scheduler: Option[Cancellable] = None

  def order() {
    actor ! newOrder
  }

  def run() {
    if(!scheduler.isDefined)
      scheduler = Option(simulator.actorSystem.scheduler.schedule(1 seconds, 30 seconds, actor, newOrder))
  }

  def stop() {
    scheduler.foreach(_.cancel())
    scheduler = None
  }

  /**
   * Creates a new order with a dummy connection
   */
  private def newOrder =
    Order(
      date = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat,
      connection = Connection("Initial", product, suppliers.head, oemSupplier),
      count = 1
    )
}

object Network {

  private val random = new Random(0)

  def build(product: Product)(implicit simulator: Simulator): Network = {
    val suppliers = for(part <- product :: product.partList) yield generateSupplier(part)
    val connections = new SimpleNetworkBuilder(suppliers).apply(product)
    val actors = for(supplier <- suppliers) yield createActor(supplier)

    new Network(simulator, actors.head, product, suppliers, connections)
  }

  def createActor(supplier: Supplier)(implicit simulator: Simulator): ActorRef = {
    simulator.actorSystem.actorOf(Props(classOf[SupplierActor], supplier, simulator), supplier.uri)
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
