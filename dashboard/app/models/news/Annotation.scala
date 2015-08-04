package models.news

/**
 * Created by rene on 29.06.15.
 */
case class Annotation(annClass: String, meansUri: String, body: String) {

}

/*

PREFIX art: <http://www.xybermotive.com/news/Article/>
PREFIX sno: <http://www.xybermotive.com/newsOnt/>
PREFIX ann: <http://www.w3.org/2000/10/annotation-ns#>
PREFIX schema: <http://schema.org/>
PREFIX sc: <http://www.xybermotive.com/ontology/>

SELECT ?subject ?headline ?text ?image ?body ?annClass ?beginIndex ?endIndex ?means WHERE {
  ?search sno:subject ?subject .
  ?search schema:result ?article .
  ?article schema:text ?text .
  ?article schema:image ?image .
  ?article schema:headline ?headline .
  ?article sno:isFetched true .
  ?article sno:annotation ?ann .
  ?ann a ?annClass .
  ?ann ann:body ?body .
  ?ann <http://ns.aksw.org/scms/beginIndex> ?beginIndex .
  ?ann <http://ns.aksw.org/scms/endIndex> ?endIndex .
  ?ann <http://ns.aksw.org/scms/means> ?means .
  # annType: String, beginIndex: Int, endIndex: Int, meansUri: String, body: String
}

 */