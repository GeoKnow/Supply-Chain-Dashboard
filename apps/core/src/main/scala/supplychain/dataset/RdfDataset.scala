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

  def addSupplier(supplier: Supplier) {
     insert(s"""
     | <${supplier.uri}> a sc:Supplier ;
     |                   sc:name "${supplier.name}" ;
     |                   sc:street "${supplier.address.street}" ;
     |                   sc:zipcode "${supplier.address.zipcode}" ;
     |                   sc:city "${supplier.address.city}" ;
     """)
  }

  def addConnection(c: Connection) {
    insert(s"""
     | <${c.uri}> a sc:Connection ;
     |            sc:product "${c.content.name}" ;
     |            sc:sender <${c.sender.uri}> ;
     |            sc:receiver <${c.receiver.uri}> .
     """)
  }

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

  private def insert(statements: String) {
    val query =
      s"""
        | PREFIX sc: <${Namespaces.schema}>
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

  def query(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execSelect()
  }

  def describe(queryStr: String) = {
    val query = QueryFactory.create(queryStr)
    QueryExecutionFactory.create(query, model).execDescribe()
  }

}
