package models

import models.news.{Annotation, News}
import supplychain.dataset.{EndpointConfig}
import supplychain.model.{Supplier, DateTime}
import scala.collection.JavaConversions._

/**
 * Created by rene on 30.06.15.
 */
class NewsProvider(ec: EndpointConfig) {

  def getNews(supplier: Supplier, date: DateTime): List[News] = {
    val query =
      s"""
         |PREFIX art: <http://www.xybermotive.com/news/Article/>
         |PREFIX sno: <http://www.xybermotive.com/newsOnt/>
         |PREFIX ann: <http://www.w3.org/2000/10/annotation-ns#>
         |PREFIX schema: <http://schema.org/>
         |PREFIX sc: <http://www.xybermotive.com/ontology/>
         |PREFIX suppl: <http://www.xybermotive.com/supplier/>
         |
         |SELECT DISTINCT ?artUrl ?headline ?means ?body ?image ?text ?annClass WHERE {
         |  ?search sno:subject <${supplier.uri}> .
         |  ?search sno:beginDate ?beginDate .
         |  FILTER ( STRSTARTS( ?beginDate, "${date.toFormat("yyyy-MM-dd")}") ) .
         |  ?search schema:result ?article .
         |  ?article schema:headline ?headline .
         |  FILTER ( STR(?headline) != "" ) .
         |  ?article schema:url ?artUrl .
         |  OPTIONAL { ?article schema:image ?image . }
         |  OPTIONAL { ?article schema:text ?text . }
         |  OPTIONAL {
         |    ?article sno:annotation ?ann .
         |    ?ann a ?annClass .
         |    FILTER( STR(?annClass) != "http://www.w3.org/2000/10/annotation-ns#Annotation") .
         |    ?ann <http://ns.aksw.org/scms/means> ?means .
         |    ?ann <http://www.w3.org/2000/10/annotation-ns#body> ?body .
         |  }
         |}
       """.stripMargin

    val res = ec.createEndpoint().select(query).toList

    for((artUrl, artResults) <- res.groupBy(_.getResource("artUrl").getURI).toList) yield {
      val art = artResults.head
      News (
        subjectUri = supplier.uri,
        uri = artUrl,
        headline = Option(art.getLiteral("headline")).map(_.getString),
        text = Option(art.getLiteral("text")).map(_.getString),
        image = Option(art.getResource("image")).map(_.getURI),
        annotations =
          for (r <- artResults if r.contains("annClass")) yield {
            Annotation(
              annClass = r.getResource("annClass").getURI,
              meansUri = r.getResource("means").getURI,
              body = r.getLiteral("body").getString
            )
          }
      )
    }
  }
}
