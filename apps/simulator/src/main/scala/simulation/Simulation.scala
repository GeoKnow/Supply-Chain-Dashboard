package simulation

object Simulation extends App {

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

  private var shippings = Seq[Shipping]()

  private var listeners = Seq[Shipping => Unit]()

  start()

  def start() {
    Network.build(product)
  }

  def addListener(listener: Shipping => Unit) {
    listeners = listeners :+ listener
  }

  def addShipping(shipping: Shipping) = {
    for(listener <- listeners)
      listener(shipping)
  }
}
