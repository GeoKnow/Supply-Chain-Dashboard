package supplychain.dataset

import supplychain.model._
import java.util.logging.Logger
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.query.{QueryParseException, QueryExecutionFactory, QueryFactory}
import com.hp.hpl.jena.update.UpdateAction
import supplychain.model.Connection
import supplychain.model.Supplier
import supplychain.model.Order

class RdfDataset {

  private val log = Logger.getLogger(getClass.getName)

  // Create new model
  private val model = ModelFactory.createDefaultModel()

  /**
   * Adds a product to the RDF data set.
   */
  def addProduct(product: Product) {
    val properties =
      s"""
       | <${product.uri}> a sc:Product ;
       |                   sc:name "${product.name}" ;
       |                   sc:count "${product.count}" ;
       """

    val parts =
      for(part <- product.parts) yield
        s"|                   sc:part <${part.uri}> ;"

    var statements = properties + parts.mkString("\n")
    statements = statements.updated(statements.lastIndexOf(';'), '.')
    insert(statements)

    for(part <- product.parts)
      addProduct(part)
  }

  /**
   * Adds a supplier to the RDF data set.
   */
  def addSupplier(supplier: Supplier) {
     insert(s"""
     | <${supplier.uri}> a sc:Supplier ;
     |                   sc:name "${supplier.name}" ;
     |                   sc:street "${supplier.address.street}" ;
     |                   sc:zipcode "${supplier.address.zipcode}" ;
     |                   sc:city "${supplier.address.city}" ;
     |                   sc:product "${supplier.product.uri}" ;
     |                   geo:lat "${supplier.coords.lat}" ;
     |                   geo:lon "${supplier.coords.lon}" ;
     """)
  }

  /**
   * Adds a connection to the RDF data set.
   */
  def addConnection(c: Connection) {
    insert(s"""
     | <${c.uri}> a sc:Connection ;
     |            sc:product "${c.content.name}" ;
     |            sc:sender <${c.sender.uri}> ;
     |            sc:receiver <${c.receiver.uri}> .
     """)
  }

  /**
   * Adds a new message to the RDF data set.
   */
  def addMessage(msg: Message) { msg match {
    case Order(uri, date, connection, count) =>
      insert(s"""
        |  <${msg.uri}> a sc:Order ;
        |               sc:date "$date" ;
        |               sc:connection <${connection.uri}> ;
        |               sc:count "$count" .
        """)

    case Shipping(uri, date, connection, count) =>
      insert(s"""
        |  <${msg.uri}> a sc:Shipping ;
        |               sc:date "$date" ;
        |               sc:connection <${connection.uri}> ;
        |               sc:count "$count" .
        """)
  }}

  /**
   * Inserts a number of statements into the RDF data set.
   */
  private def insert(statements: String) {
    val query =
      s"""
        | PREFIX sc: <${Namespaces.schema}>
        | PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
        | INSERT DATA {
        $statements
        | }
      """.stripMargin

    try {
      UpdateAction.parseExecute(query, model)
    } catch {
      case ex: QueryParseException =>
        log.severe("The following Query is malformed:\n" + query)
        throw ex
    }
  }

  /**
   * Executes a SPARQL Select query on the data set.
   */
  def query(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execSelect()
  }

  /**
   * Executes a SPARQL Describe query on the data set.
   */
  def describe(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execDescribe()
  }

}
