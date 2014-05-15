package supplychain.simulation

import akka.actor.{ActorRef, ActorSystem}
import supplychain.model._
import supplychain.model.Product
import supplychain.model.Supplier
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.util.Timeout

object Simulation {

  val product =
    Product(
      name = "Chair",
      parts =
        Product(
          name = "Legs",
          count = 4
        ) ::
          Product(
            name = "Back",
            parts = Product("Side_Rails", 2) :: Product("Back_Support", 1) :: Nil
          ) :: Nil
    )

  //val product = Product("Product", parts = Product("Part") :: Nil)

  private[simulation] val system = ActorSystem("system")

  private val network = Network.build(product)

  @volatile
  private var listeners = Seq[Message => Unit]()

  def order() = {
    network.order()
  }

  def suppliers: Seq[Supplier] = network.suppliers

  def connections: Seq[Connection] = network.connections

  def addListener(listener: Message => Unit) {
    listeners = listeners :+ listener
  }

  // TODO should be synchronized in listeners
  def addMessage(msg: Message) = synchronized {
    for(listener <- listeners)
      listener(msg)
  }

  def getActor(supplier: Supplier) = {
    system.actorSelection("/user/" + supplier.uri)
  }
}
