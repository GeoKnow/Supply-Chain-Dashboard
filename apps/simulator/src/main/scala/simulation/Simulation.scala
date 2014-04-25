package simulation

import dataset._
import akka.actor.ActorSystem
import scala.Product
import dataset.Supplier
import dataset.Shipping

class Simulation {

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

  private[simulation] val system = ActorSystem("system")

  private val network = Network.build(product, this)
  network.start(this)

  @volatile
  private var listeners = Seq[Shipping => Unit]()

  def suppliers: Seq[Supplier] = network.suppliers

  def connections: Seq[Connection] = network.connections

  def addListener(listener: Shipping => Unit) {
    listeners = listeners :+ listener
  }

  def addShipping(shipping: Shipping) = {
    for(listener <- listeners)
      listener(shipping)
  }

  def getActor(supplier: Supplier) = system.actorSelection("/user/" + supplier.uri)
}
