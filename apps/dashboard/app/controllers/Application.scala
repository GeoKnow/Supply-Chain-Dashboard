package controllers

import play.api.mvc._
import models.CurrentDataset
import play.api.libs.iteratee.Concurrent
import play.api.libs.Comet
import play.api.libs.Comet.CometMessage
import scala.util.Random
import dataset.{Shipping, Connection}

object Application extends Controller {

  def print() {
  }

  def index = Action {
    print()
    Ok(views.html.index())
  }

  def query = Action {
    Ok(views.html.query())
  }

  def map = Action {
    Ok(views.html.map())
  }

  def deliveryStream = Action {
    val (enumerator, channel) = Concurrent.broadcast[Shipping]

    val listener = (delivery: Shipping) => channel.push(delivery)

    CurrentDataset().addListener(listener)

    implicit val deliveryMessage = CometMessage[Shipping](d => s"'${d.connection.id}'")

    Ok.chunked(enumerator &> Comet(callback = "parent.addShipping"))
  }

}