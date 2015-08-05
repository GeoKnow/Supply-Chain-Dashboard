package supplychain.simulator

import java.util.logging.Logger

import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor.{ActorSystem, Cancellable, Props}
import supplychain.dataset.{Dataset, RdfDataset}
import supplychain.model.{DateTime, Connection, Message, Supplier}
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

  // get the application configuration from endpoint
  private val cp = new ConfigurationProvider(Configuration.get.endpointConfig, wp, Configuration.get.productUri)
  val product = cp.getProduct()

  // Generate the supply chain network
  private val network = Network.build(product, wp, cp)

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


  // continues from pause and/or resets the interval
  def start(interval: Double): Unit = {
    pause()
    metronom = Option(actorSystem.scheduler.schedule(0 seconds, interval.seconds, scheduler, Scheduler.Tick))
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
  def step(start: Option[DateTime]) {
    for (s <- start) {
      simulator.shutdown
      simulator = new Simulator(Akka.system)

      simulator.startDate = s
      simulator.currentDate = s
    }
    simulator.scheduler ! Scheduler.Tick
  }

  // starts a new simulation with the given parameters
  def run(interval: Double, startDate: Option[DateTime], endDate: Option[DateTime]) {
    simulator.pause()
    for (s <- startDate) {
      simulator.shutdown
      simulator = new Simulator(Akka.system)
      simulator.startDate = s
      simulator.currentDate = s
    }
    for (e <- endDate) simulator.simulationEndDate = e
    simulator.metronom = Option(simulator.actorSystem.scheduler.schedule(0 seconds, interval.seconds, simulator.scheduler, Scheduler.Tick))
  }

  def apply() = simulator
}