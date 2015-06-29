package supplychain.news

import supplychain.model.Supplier

/**
 * Created by rene on 29.06.15.
 */
case class News(subject: Supplier, headline: String, text: String, image: String, newsType: String, annotations: List[Annotation]) {

}
