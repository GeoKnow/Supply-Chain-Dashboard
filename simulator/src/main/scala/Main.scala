object Main extends App {

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

    Network.build(product)
}

