package supplychain.simulator

import com.hp.hpl.jena.query.{QueryExecutionFactory, QueryFactory}
import supplychain.model.WeatherStation
import scala.collection.JavaConversions._

/**
 * Created by rene on 08.01.15.
 */
object Test extends App {

  var queryStr =
    """
        |PREFIX gkwo: <http://www.xybermotive.com/GeoKnowWeatherOnt#>
        |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
        |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        |
        |SELECT ?s ?label ?long ?lat FROM <http://www.xybermotive.com/GeoKnowWeather#>
        |WHERE {
        |    ?s a gkwo:WeatherStation ;
        |        geo:long ?long ;
        |        geo:lat ?lat ;
        |        gkwo:stationId ?sid ;
        |        rdfs:label ?label .
        |    ?s gkwo:hasObservation ?obs .
        |}
        |GROUP BY ?s ?label ?long ?lat
        |HAVING (count(?obs) > 1200)
        |ORDER BY ASC (<bif:st_distance> (<bif:st_point>(?long, ?lat), <bif:st_point>(8.5, 50.5))) LIMIT 1
    """.stripMargin

  var query = QueryFactory.create(queryStr)

  var result = QueryExecutionFactory.sparqlService("http://192.168.59.103/sparql", query).execSelect().toSeq

  for(binding <- result) {
    println(binding.toString)
    println(binding.getLiteral("label").getString)
    println(binding.getResource("s").toString)
    println(binding.getLiteral("long").getDouble)
    println(binding.getLiteral("lat").getDouble)
    println(binding.getLiteral("sid").getString)
  }

  queryStr =
    """
         |PREFIX gkwo: <http://www.xybermotive.com/GeoKnowWeatherOnt#>
         |PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |SELECT ?obsuri ?date ?tmin ?tmax ?prcp ?snwd FROM <http://www.xybermotive.com/GeoKnowWeather#>
         |WHERE {
         |  <http://www.xybermotive.com/GeoKnowWeather#GME00111430> gkwo:hasObservation ?obsuri .
         |  ?obsuri gkwo:date ?date .
         |  OPTIONAL { ?obsuri gkwo:tmin ?tmin . }
         |  OPTIONAL { ?obsuri gkwo:tmax ?tmax . }
         |  OPTIONAL { ?obsuri gkwo:prcp ?prcp . }
         |  OPTIONAL { ?obsuri gkwo:snwd ?snwd . }
         |}
       """.stripMargin

  println(queryStr)

  query = QueryFactory.create(queryStr)

  result = QueryExecutionFactory.sparqlService("http://192.168.59.103/sparql", query).execSelect().toSeq


  for (binding <- result) {
    println(binding.toString)
    val obsuri = binding.getResource("obsuri").toString
    val date = binding.getLiteral("date").getValue

    val tmin = if (binding.get("tmin") != null) binding.getLiteral("tmin").getFloat else -9999.0
    val tmax = if (binding.get("tmax") != null) binding.getLiteral("tmax").getFloat else -9999.0
    val prcp = if (binding.get("prcp") != null) binding.getLiteral("prcp").getFloat else -9999.0
    val snwd = if (binding.get("snwd") != null) binding.getLiteral("snwd").getFloat else -9999.0
    //ws = new WeatherObservation()

    println(obsuri)
    println(date)
    println(tmin)
    println(tmax)
    println(prcp)
    println(snwd)
  }

}
