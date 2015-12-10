package supplychain.simulator

import java.net.URLEncoder
import java.util.logging.Logger

import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor.{ActorSystem, Cancellable, Props}
import supplychain.dataset.{WeatherProvider, ConfigurationProvider, Dataset, RdfDataset}
import supplychain.model._
import supplychain.model.{Duration=>SCDuration}
import supplychain.simulator.exceptions.SimulationPeriodOutOfBoundsException
import supplychain.simulator.network.Network

import scala.collection.mutable
import scala.concurrent.duration._

/**
 * The supply chain simulator.
 */
class Simulator(val actorSystem: ActorSystem, productUri: String) extends Dataset {

  private val log = Logger.getLogger(classOf[Simulator].getName)

  // The simulation start date
  @volatile
  var startDate: DateTime = Configuration.get.minStartDate

  // The current simulation date
  @volatile
  var currentDate: DateTime = Configuration.get.minStartDate

  // the date the simulation should stop
  @volatile
  var simulationEndDate: DateTime = Configuration.get.maxEndDate

  // The Actors Message Queue
  @volatile
  var messageQueue = mutable.PriorityQueue[Message]()(Ordering.by(-_.date.milliseconds))

  //get the weather data provider
  private val wp = new WeatherProvider(Configuration.get.endpointConfig)

  // get the application configuration from endpoint
  private val cp = new ConfigurationProvider(Configuration.get.endpointConfig, wp)
  val product = cp.getProduct(productUri)

  // Generate the supply chain network
  val network = Network.build(product, wp, cp)

  val b64Enc = new sun.misc.BASE64Encoder()


  // Create actors for all suppliers
  val supplierActors = for(supplier <- network.suppliers) yield
    actorSystem.actorOf(Props(classOf[SupplierActor], supplier, this, wp), URLEncoder.encode(supplier.uri, "UTF8"))

  // The message scheduler.
  val scheduler = actorSystem.actorOf(Props(classOf[Scheduler], network.rootConnection, this), "Scheduler")

  // The metronom send ticks to the scheduler for advancing the simulation.
  private var metronom: Option[Cancellable] = None

  // Listeners for intercepted messages
  @volatile
  private var listeners = Seq[SimulationUpdate => Unit]()

  // The simulation interval between two ticks
  val tickInterval = SCDuration.days(Configuration.get.tickIntervalsDays)

  // List of new messages.
  var newMessages = Seq[Message]()

  // List of past messages.
  var messages = Seq[Message]()

  private val dataset = new RdfDataset(Configuration.get.endpointConfig, Configuration.get.silkProject)

  private var isSupplierNetworkDataWritten = false

  def writeSupplierNetworkData() = {
    // for legacy reasons write this data
    // TODO: remove this duplicated Data
    dataset.addProduct(product)
    for (supplier <- suppliers) dataset.addSupplier(supplier)
    for (connection <- connections) dataset.addConnection(connection)
  }

  // List of suppliers.
  def suppliers: Seq[Supplier] = network.suppliers

  // List of connections between suppliers.
  def connections: Seq[Connection] = network.connections

  override def addListener(listener: SimulationUpdate => Unit) {
    listeners = listeners :+ listener
  }

  def query(queryStr: String) = dataset.query(queryStr)

  def describe(queryStr: String) = dataset.describe(queryStr)

  // TODO should be synchronized in listeners
  private[simulator] def addMessage(msg: Message) = synchronized {
    messages = messages :+ msg
    newMessages = newMessages :+ msg
    dataset.addMessage(msg)
  }

  def loadMessages() = {
    if (messages.isEmpty) {
      messages = dataset.getMessages(startDate, simulationEndDate, network.connections)
    }
  }

  def advanceSimulation(): Unit = {
    if (!isSupplierNetworkDataWritten) {
      writeSupplierNetworkData()
      isSupplierNetworkDataWritten = true
    }
    if (currentDate <= simulationEndDate) {
      scheduler ! Scheduler.Tick(currentDate)
      currentDate += tickInterval
      val su = SimulationUpdate(currentDate, newMessages)
      for(listener <- listeners)
        listener(su)
      newMessages = Seq.empty
    } else {
      pause()
    }
  }

  // continues from pause and/or resets the interval
  def start(interval: Double): Unit = {
    pause()
    metronom = Option(actorSystem.scheduler.schedule(0 seconds, interval.seconds)(advanceSimulation))
  }

  // pauses the simulation
  def pause() {
    metronom.foreach(_.cancel())
    metronom = None
  }

  def shutdown() {
    actorSystem.stop(scheduler)
    for(a <- supplierActors)
      actorSystem.stop(a)
  }

  def scheduleMessage(msg: Message) {
    scheduler ! msg
  }

  def getActor(supplier: Supplier) = {
    actorSystem.actorSelection("/user/" + URLEncoder.encode(supplier.uri, "UTF8"))
  }

}

object Simulator {
  private var simulator:Simulator = null

  private def reinitSimulation(productUri: Option[String], graphUri: Option[String]): Unit = {
    if (simulator != null) {
      simulator.pause()
      simulator.shutdown
    }

    for (p <- productUri) {
      Configuration.get.productUri = p
    }
    for (g <- graphUri) {
      Configuration.get.endpointConfig.defaultGraph = g
    }

    simulator = new Simulator(Akka.system, Configuration.get.productUri)
  }

  // advance the simulation for a single tick
  def step(start: Option[DateTime], productUri: Option[String], graphUri: Option[String]) {
    reinitSimulation(productUri, graphUri)

    for (s <- start) {
      if (s < Configuration.get.minStartDate) {
        val msg = "start date may not be before: " + Configuration.get.minStartDate.toYyyyMMdd()
        simulator.log.info(msg)
        throw new SimulationPeriodOutOfBoundsException(msg)
      }
      simulator.startDate = s
      simulator.currentDate = s
    }
    simulator.advanceSimulation()
  }

  // starts a new simulation with the given parameters
  def run(interval: Double, startDate: Option[DateTime], endDate: Option[DateTime], productUri: Option[String], graphUri: Option[String]) {
    reinitSimulation(productUri, graphUri)

    for (s <- startDate) {
      if (s < Configuration.get.minStartDate) {
        val msg = "start date may not be before: " + Configuration.get.minStartDate.toYyyyMMdd()
        simulator.log.info(msg)
        throw new SimulationPeriodOutOfBoundsException(msg)
      }

      simulator.startDate = s
      simulator.currentDate = s
    }
    for (e <- endDate) {
      if (e > Configuration.get.maxEndDate) {
        val msg = "end date may not be after: " + Configuration.get.maxEndDate.toYyyyMMdd()
        simulator.log.info(msg)
        throw new SimulationPeriodOutOfBoundsException(msg)
      }
      simulator.simulationEndDate = e
    }
    simulator.metronom = Option(simulator.actorSystem.scheduler.schedule(0 seconds, interval.seconds)(simulator.advanceSimulation))
  }

  def apply() = simulator

  def isSimulationRunning(): Boolean = {
    var simulationIsRunning = false
    for (c <- simulator.metronom) simulationIsRunning = (!c.isCancelled)
    var dueEnqueuedMsgs = false
    if (!simulator.messageQueue.isEmpty && simulator.messageQueue.head.date <= Simulator().simulationEndDate)
        dueEnqueuedMsgs = true
    if (simulationIsRunning || dueEnqueuedMsgs) true
    else false
  }
}

