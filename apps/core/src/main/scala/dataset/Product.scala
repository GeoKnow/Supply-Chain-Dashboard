package dataset

case class Product(name: String, count: Int = 1, parts: List[Product] = Nil) {

  /**
   * Generates a list of all parts of this product, including parts of parts.
   */
  def partList: List[Product] = parts ::: parts.flatMap(_.partList)
}
