package supplychain.simulator

import java.util.logging.Logger

import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor.{ActorSystem, Cancellable, Props}
import supplychain.dataset.{WeatherProvider, ConfigurationProvider, Dataset, RdfDataset}
import supplychain.model._
import supplychain.model.{Duration=>SCDuration}
import supplychain.simulator.network.Network

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * The supply chain simulator.
 */
class Simulator(val actorSystem: ActorSystem) extends Dataset {

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

  //get the weather data provider
  private val wp = new WeatherProvider(Configuration.get.endpointConfig)

  private var productUri: Option[String] = _
  private var graphUri: Option[String] = _

  // get the application configuration from endpoint
  private val cp = new ConfigurationProvider(Configuration.get.endpointConfig, wp, Configuration.get.productUri)
  val product = cp.getProduct()

  // Generate the supply chain network
  val network = Network.build(product, wp, cp)

  // Create actors for all suppliers
  val supplierActors = for(supplier <- network.suppliers) yield
    actorSystem.actorOf(Props(classOf[SupplierActor], supplier, this, wp), supplier.id)

  // The message scheduler.
  val scheduler = actorSystem.actorOf(Props(classOf[Scheduler], network.rootConnection, this), "Scheduler")

  // The metronom send ticks to the scheduler for advancing the simulation.
  private var metronom: Option[Cancellable] = None

  // Listeners for intercepted messages
  @volatile
  private var listeners = Seq[Message => Unit]()

  // The simulation interval between two ticks
  val tickInterval = SCDuration.days(Configuration.get.tickIntervalsDays)

  // List of past messages.
  var messages = Seq[Message]()

  // write generated product / supplier-network to endpoint
  private val dataset = new RdfDataset(Configuration.get.endpointConfig)

  // for legacy reasons write this data
  // TODO: remove this duplicated Data
  //dataset.addProduct(sim.product)
  dataset.addProduct(product)
  for(supplier <- suppliers) dataset.addSupplier(supplier)
  for(connection <- connections) dataset.addConnection(connection)

  // List of suppliers.
  def suppliers: Seq[Supplier] = network.suppliers

  // List of connections between suppliers.
  def connections: Seq[Connection] = network.connections

  override def addListener(listener: Message => Unit) {
    listeners = listeners :+ listener
  }

  def query(queryStr: String) = dataset.query(queryStr)

  def describe(queryStr: String) = dataset.describe(queryStr)

  // TODO should be synchronized in listeners
  private[simulator] def addMessage(msg: Message) = synchronized {
    messages = messages :+ msg
    dataset.addMessage(msg)
    for(listener <- listeners)
      listener(msg)
  }

  def advanceSimulation(): Unit = {
    if (currentDate <= simulationEndDate) {
      scheduler ! Scheduler.Tick(currentDate)
      currentDate += tickInterval
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
    actorSystem.actorSelection("/user/" + supplier.id)
  }
}

object Simulator {
  private var simulator = new Simulator(Akka.system)

  // advance the simulation for a single tick
  def step(start: Option[DateTime], productUri: Option[String], graphUri: Option[String]) {
    simulator.pause()

    reinitSimulation(productUri, graphUri)

    for (s <- start) {
      simulator.shutdown
      simulator = new Simulator(Akka.system)

      simulator.startDate = s
      simulator.currentDate = s
    }
    simulator.advanceSimulation()
  }

  private def reinitSimulation(productUri: Option[String], graphUri: Option[String]): Unit = {
    if (productUri != simulator.productUri || graphUri != simulator.graphUri) {
      simulator.productUri = productUri
      for (p <- productUri) Configuration.get.productUri = p
      simulator.graphUri = graphUri
      for (g <- graphUri) Configuration.get.endpointConfig.defaultGraph = g
      simulator.shutdown
      simulator = new Simulator(Akka.system)
    }
  }

  // starts a new simulation with the given parameters
  def run(interval: Double, startDate: Option[DateTime], endDate: Option[DateTime], productUri: Option[String], graphUri: Option[String]) {
    simulator.pause()

    reinitSimulation(productUri, graphUri)

    for (s <- startDate) {
      simulator.shutdown
      simulator = new Simulator(Akka.system)
      simulator.startDate = s
      simulator.currentDate = s
    }
    for (e <- endDate) simulator.simulationEndDate = e
    simulator.metronom = Option(simulator.actorSystem.scheduler.schedule(0 seconds, interval.seconds)(simulator.advanceSimulation))
  }

  def apply() = simulator

  def isSimulationRunning(): Boolean = {
    simulator.metronom != None
  }
}