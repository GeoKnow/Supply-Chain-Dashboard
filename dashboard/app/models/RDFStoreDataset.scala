package models

import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}

import com.hp.hpl.jena.query.ResultSet
import com.hp.hpl.jena.rdf.model.Model
import play.api.Logger
import supplychain.dataset.{ConfigurationProvider, Dataset, WeatherProvider}
import supplychain.model._

/**
 * Created by rene on 28.08.15.
 */
object RdfStoreDataset extends Dataset {

  val epc = Configuration.get.endpointConfig
  val wp = new WeatherProvider(epc)
  val cp = new ConfigurationProvider(epc, wp)
  var product = cp.getProduct(Configuration.get.productUri)
  val ep = epc.getEndpoint()
  private var messagesCache = Seq[Message]()

  // Listeners for intercepted messages
  @volatile
  private var listeners = Seq[SimulationUpdate => Unit]()

  override def addListener(listener: SimulationUpdate => Unit) {
    listeners = listeners :+ listener
  }

  /** The list of suppliers. */
  override lazy val suppliers: Seq[Supplier] = {
    for(part <- product :: product.partList) yield cp.getSupplier(part)
  }

  override def describe(queryStr: String): Model = {
    ep.describe(queryStr)
  }

  /**
   * Messages that have been exchanged between suppliers along a connection.
   * Messages are ordered by date.
   */
  override def messages: Seq[Message] = {
    messagesCache
  }

  /** The connections between suppliers. */
  override lazy val connections: Seq[Connection] = {
    cp.getConnections(suppliers)
  }

  override def query(queryStr: String): ResultSet = {
    ep.select(queryStr)
  }


  object Scheduler {

    val stse = Executors.newSingleThreadScheduledExecutor()

    var sf: Option[ScheduledFuture[_]] = None
    var currentDate = Configuration.get.minStartDate
    var tickInterval = Configuration.get.tickIntervalsDays

    def start(date: Option[DateTime], interval: Double = 1.0) = {
      for (d <- date) currentDate = d
      sf = Some(stse.scheduleAtFixedRate(new Runnable {
        override def run(): Unit = step
      }, 0, (interval * 1000).toLong, TimeUnit.MILLISECONDS))
    }

    def pause() = {
      Logger.info("pause() called")
      for (s <- sf) {
        s.cancel(false)
      }
    }

    def step() = {
      if (currentDate <= Configuration.get.maxEndDate) {
        val msgs = cp.getMessages(currentDate, currentDate + Duration.days(tickInterval), connections)
        val su = SimulationUpdate(currentDate, msgs)

        for (l <- listeners) {
          l(su)
        }

        messagesCache ++= msgs
        currentDate += Duration.days(tickInterval)
      } else {
        pause()
      }
    }
  }

}
