package supplychain.simulator

import com.hp.hpl.jena.query.ResultSet
import supplychain.dataset.Dataset
import supplychain.model.{Product, Connection, Supplier, Message}
import akka.actor.ActorSystem

/**
 * The supply chain simulator.
 */
object Simulator extends Dataset {

  // The simulation to run
  private val sim = CarSimulation

  // The akka actor system
  private[simulator] val system = ActorSystem("system")

  // The supply chain network
  private val network = Network.build(sim.product)

  // Listeners for intercepted messages
  @volatile
  private var listeners = Seq[Message => Unit]()

  // List of past messages.
  var messages = Seq[Message]()

  // List of suppliers.
  def suppliers: Seq[Supplier] = network.suppliers

  // List of connections between suppliers.
  def connections: Seq[Connection] = network.connections

  override def addListener(listener: Message => Unit) {
    listeners = listeners :+ listener
  }

  def query(queryStr: String): ResultSet = throw new UnsupportedOperationException()

  // TODO should be synchronized in listeners
  private[simulator] def addMessage(msg: Message) = synchronized {
    messages = messages :+ msg
    for(listener <- listeners)
      listener(msg)
  }

  def order() = {
    network.order()
  }

  def run() = {
    network.run()
  }

  def stop() = {
    network.stop()
  }

  def getActor(supplier: Supplier) = {
    system.actorSelection("/user/" + supplier.uri)
  }
}
