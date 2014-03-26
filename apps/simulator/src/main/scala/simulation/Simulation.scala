package simulation

import dataset.{Delivery, Address}

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

  private lazy val network = Network.build(product, this)

  @volatile
  private var listeners = Seq[Delivery => Unit]()

  def suppliers: Seq[Address] = network.suppliers

  def addListener(listener: Delivery => Unit) {
    listeners = listeners :+ listener
  }

  def addDelivery(shipping: Delivery) = {
    for(listener <- listeners)
      listener(shipping)
  }
}
