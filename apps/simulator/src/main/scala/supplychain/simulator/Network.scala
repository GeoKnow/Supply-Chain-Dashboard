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
import supplychain.dataset.Namespaces

class Network(simulator: Simulator, val actor: ActorRef, val product: Product, val suppliers: Seq[Supplier], val connections: Seq[Connection]) {

  // Create an OEM actor that is responsible for sending the initial orders for the product
  private val rootSupplier = Network.generateSupplier(Product("OEM", parts = product :: Nil))
  val rootConnection = Connection("Initial", product, suppliers.head, rootSupplier)
  Network.createActor(rootSupplier)(simulator)

  private val scheduler = simulator.actorSystem.actorOf(Props(classOf[Scheduler], rootConnection, simulator), "Scheduler")

  private var metronom: Option[Cancellable] = None
  
  def step() {
    scheduler ! Scheduler.Tick
  }

  def run() {
    if(!metronom.isDefined)
      metronom = Option(simulator.actorSystem.scheduler.schedule(1 seconds, 1 seconds, scheduler, Scheduler.Tick))
  }

  def stop() {
    metronom.foreach(_.cancel())
    metronom = None
  }

  def schedule(msg: Message) {
    scheduler ! msg
  }

//  /**
//   * Creates a new order with a dummy connection.
//   */
//  private def createOrder = {
//    val connection = Connection("Initial", product, suppliers.head, oemSupplier)
//    val date = DateTime.now
//    Order(
//      date = date,
//      connection = connection,
//      count = 10
//    )
//  }
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
    simulator.actorSystem.actorOf(Props(classOf[SupplierActor], supplier, simulator), supplier.id)
  }

  def generateSupplier(product: Product) = {
    val lat = 47 + 7 * random.nextDouble()
    val lon = 6 + 9 * random.nextDouble()
    val coordinates = Coordinates(lat, lon)
    val address = Address("Beispielstr. 1", "10123", "Musterstadt", "Deutschland")
    Supplier(
      Namespaces.supplier + product.name + "-" + UUID.randomUUID.toString,
      product.name + " Supplier", address, coordinates, product)
  }
}
