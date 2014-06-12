package supplychain.simulator

import com.hp.hpl.jena.query.ResultSet
import supplychain.dataset.{RdfDataset, Dataset}
import supplychain.model.{Product, Connection, Supplier, Message}
import akka.actor.ActorSystem
import scala.collection.immutable.Queue

/**
 * The supply chain simulator.
 */
class Simulator(val actorSystem: ActorSystem) extends Dataset {

  // The simulation to run
  private val sim = FairPhoneSimulation // CarSimulation

  // The supply chain network
  private val network = Network.build(sim.product)(this)

  // Listeners for intercepted messages
  @volatile
  private var listeners = Seq[Message => Unit]()

  // List of past messages.
  var messages = Seq[Message]()

  private val dataset = new RdfDataset()
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

  def scheduleMessage(msg: Message) = {
    network.schedule(msg)
  }

  def step() = {
    network.step()
  }

  def run(frequency: Double) = {
    network.run(frequency)
  }

  def stop() = {
    network.stop()
  }

  def getActor(supplier: Supplier) = {
    actorSystem.actorSelection("/user/" + supplier.id)
  }
}