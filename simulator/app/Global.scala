import play.api.mvc.{Result, RequestHeader, Filter, WithFilters}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rene on 14.09.15.
 */
object Global extends WithFilters(CorsFilter) {

}

object CorsFilter extends Filter {
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    for(res <- next(request)) yield {
      res.withHeaders(("Access-Control-Allow-Origin", "*"))
    }
  }
}