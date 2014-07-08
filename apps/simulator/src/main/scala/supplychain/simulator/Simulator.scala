package supplychain.simulator

import akka.actor.{ActorSystem, Cancellable, Props}
import supplychain.dataset.{Dataset, RdfDataset}
import supplychain.model.{Connection, Message, Supplier}
import supplychain.simulator.network.Network

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * The supply chain simulator.
 */
class Simulator(val actorSystem: ActorSystem, endpointUrl: String) extends Dataset {

  // The simulation to run
  private val sim = FairPhoneSimulation // CarSimulation

  // Generate the supply chain network
  private val network = Network.build(sim.product)

  // Create actors for all suppliers
  for(supplier <- network.suppliers)
    actorSystem.actorOf(Props(classOf[SupplierActor], supplier, this), supplier.id)

  // The message scheduler.
  private val scheduler = actorSystem.actorOf(Props(classOf[Scheduler], network.rootConnection, this), "Scheduler")

  // The metronom send ticks to the scheduler for advancing the simulation.
  private var metronom: Option[Cancellable] = None

  // Listeners for intercepted messages
  @volatile
  private var listeners = Seq[Message => Unit]()

  // List of past messages.
  var messages = Seq[Message]()

  private val dataset = new RdfDataset(endpointUrl)
  dataset.addProduct(sim.product)
  for(supplier <- suppliers) dataset.addSupplier(supplier)
  for(connection <- connections) dataset.addConnection(connection)

  // List of suppliers.
  def suppliers: Seq[Supplier] = network.suppliers

  // List of connections between suppliers.
  def connections: Seq[Connection] = network.connections

  override def addListener(listener: Message => Unit) {
    listeners = listeners :+ listener
  }

  def query(queryStr: String) = dataset.query(queryStr)

  def describe(queryStr: String) = dataset.describe(queryStr)

  // TODO should be synchronized in listeners
  private[simulator] def addMessage(msg: Message) = synchronized {
    messages = messages :+ msg
    dataset.addMessage(msg)
    for(listener <- listeners)
      listener(msg)
  }

  def step() {
    scheduler ! Scheduler.Tick
  }

  def run(frequency: Double) {
    stop()
    metronom = Option(actorSystem.scheduler.schedule(0 seconds, frequency.seconds, scheduler, Scheduler.Tick))
  }

  def stop() {
    metronom.foreach(_.cancel())
    metronom = None
  }

  def scheduleMessage(msg: Message) {
    scheduler ! msg
  }

  def getActor(supplier: Supplier) = {
    actorSystem.actorSelection("/user/" + supplier.id)
  }
}