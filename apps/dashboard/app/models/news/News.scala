package models.news

import supplychain.model.Supplier

/**
 * Created by rene on 29.06.15.
 */
case class News(subjectUri: String, uri: String, headline: Option[String], text: Option[String], image: Option[String], annotations: List[Annotation]) {

}
