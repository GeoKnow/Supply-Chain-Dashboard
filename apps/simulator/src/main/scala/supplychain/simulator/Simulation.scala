package supplychain.simulator

import supplychain.model.Product

/**
 * Defines a simulation, i.e., its parameters.
 */
trait Simulation {
  def product: Product
}

/**
 * Simple chair example simulation.
 */
object ChairSimulation extends Simulation {

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

}

/**
 * Car simulation.
 * Currently incomplete.
 */
object CarSimulation extends Simulation {

  private def doors =
    Product(
      name = "Door",
      count = 4,
      parts = Product("Door_Handle") ::
              Product("Door_Seal", parts = Product("Door_Watershield") :: Product("Hinge") :: Nil) ::
              Product("Door_Latch") :: Nil
    )

  private def windows =
    Product(
      name = "FrontShield",
      count = 1,
      parts = Product("Front_Glass") :: Product("FrontWindowSeal") :: Nil
    ) ::
    Product(
      name = "RearShield",
      count = 1,
      parts = Product("Rear_Glass") :: Product("RearWindowSeal") :: Nil
    ) :: Nil

  val product =
    Product(
      name = "Car",
      parts = doors :: windows
    )

}