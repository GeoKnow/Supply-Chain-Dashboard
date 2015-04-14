package supplychain.dataset

import java.util.logging.Logger

import com.hp.hpl.jena.query.{QuerySolution, QueryExecutionFactory}
import supplychain.model._
import scala.collection.JavaConversions._

class RdfDataset(ec: EndpointConfig) {

  private val log = Logger.getLogger(getClass.getName)

  private var graphCreated = false

  //private var weatherStations: List[String] = List()

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
     |                   rdfs:label "${supplier.name}" ;
     |                   sc:street "${supplier.address.street}" ;
     |                   sc:zipcode "${supplier.address.zipcode}" ;
     |                   sc:city "${supplier.address.city}" ;
     |                   sc:product <${supplier.product.uri}> ;
     |                   geo:lat "${supplier.coords.lat}" ;
     |                   geo:long "${supplier.coords.lon}" ;
     |                   sc:weatherStation <${supplier.weatherStation.uri}> .
     """)
  }

  /**
   * Adds a connection to the RDF data set.
   */
  def addConnection(c: Connection) {
    insert(s"""
     | <${c.uri}> a sc:Connection ;
     |            sc:product <${c.content.uri}> ;
     |            sc:sender <${c.source.uri}> ;
     |            sc:receiver <${c.target.uri}> .
     """)

    //addWeatherStation(c.wsSource, c.source)
    //addWeatherStation(c.wsTarget, c.target)
  }

  /**
   * Adds a new message to the RDF data set.
   */
  def addMessage(msg: Message) { msg match {
    case order @ Order(uri, date, connection, count) =>
      insert(s"""
        |  <${msg.uri}> a sc:Order ;
        |               sc:date "${date.toXSDFormat}" ;
        |               sc:dueDate "${order.dueDate.toXSDFormat}" ;
        |               sc:connection <${connection.uri}> ;
        |               sc:count "$count" .
        """)
      log.info("Order date: " + date.toXSDFormat)

    case Shipping(uri, date, connection, count, order) =>
      insert(s"""
        |  <${msg.uri}> a sc:Shipping ;
        |               sc:date "${date.toXSDFormat}" ;
        |               sc:connection <${connection.uri}> ;
        |               sc:count "$count" ;
        |               sc:order <${order.uri}> .
        """)
      //addWeatherObservation(woSource)
      //addWeatherObservation(woTarget)
      log.info("Shipping date: " + date.toXSDFormat)
  }}

  /*
  def addWeatherObservation(wo: WeatherObservation) {
    insert(s"""
         | <${wo.ws.uri}> sc:hasObservation <${wo.uri}> .
         | <${wo.uri}> a sc:WeatherObservation ;
         |            sc:date "${wo.date.toXSDFormat}" ;
         |            sc:temp "${wo.temp}" ;
         |            sc:prcp "${wo.prcp}" ;
         |            sc:prcpCat "${wo.getPrcpCategory()}" ;
         |            sc:fromStation <${wo.ws.uri}> ;
         |            sc:snow "${wo.snow}" .
         """)
  } */

  /*
  def addWeatherStation(ws: WeatherStation, suppl: Supplier): Unit = {
    if (!weatherStations.contains(ws.id)) {
      insert( s"""
               | <${ws.uri}> a sc:WeatherStation ;
               |            geo:long "${ws.coords.lon}" ;
               |            geo:lat "${ws.coords.lat}" ;
               |            rdfs:label "${ws.name}" ;
               |            sc:nextTo <${suppl.uri}> ;
               |            sc:name "${ws.name}" .
               """)
      weatherStations = ws.id :: weatherStations
    }
  } */

  /**
   * Inserts a number of statements into the RDF data set.
   */
  private def insert(statements: String) {
    if(!graphCreated) {
      ec.getDefaultGraph()
      ec.getEndpoint().update(s"CREATE SILENT GRAPH <${ec.getDefaultGraph()}>")
      graphCreated = true
    }

    val query =
      s"""
        | PREFIX sc: <${Namespaces.schema}>
        | PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
        | PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        | INSERT DATA {
        |   GRAPH <${ec.getDefaultGraph()}> {
        |     $statements
        |   }
        | }
      """.stripMargin
    ec.getEndpoint().update(query)
  }

  /**
   * Executes a SPARQL Select query on the data set.
   */
  def query(queryStr: String) = {
    ec.getEndpoint().select(queryStr)
  }

  /**
   * Executes a SPARQL Select query on the data set.
   */
  def select(queryStr: String): Seq[QuerySolution] = {
    return ec.getEndpoint().select(queryStr).toSeq
  }

  /**
   * Executes a SPARQL Describe query on the data set.
   */
  def describe(queryStr: String) = {
    ec.getEndpoint().describe(queryStr)
  }


  /*
  def load(): Seq[DumpSource] = {
    val query =
      s"""
         | $queryPrefix
          | SELECT * WHERE {
          |   GRAPH <${GraphNamespaces.datasources}> {
                                                     |     ?uri a ds:DumpDataSource .
                                                     |     ?uri ds:name ?name .
                                                     |     ?uri ds:description ?description .
                                                     |   }
                                                     | }
       """.stripMargin

    val results = SparqlEndpoint.select(query)
    for(r <- results) yield {
      DumpSource(
        name = r.getLiteral("name").getString,
        description = r.getLiteral("description").getString
      )
    }
  }*/
}
