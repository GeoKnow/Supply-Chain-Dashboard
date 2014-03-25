package simulation

/**
 * Created by RobertIsele on 25.03.14.
 */
case class Product(name: String, count: Int = 1, parts: List[Product] = Nil)
