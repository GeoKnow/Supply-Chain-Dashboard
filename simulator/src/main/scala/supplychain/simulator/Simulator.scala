package supplychain.simulator

import java.util.logging.Logger

import akka.actor.{ActorSystem, Cancellable, Props}
import supplychain.dataset.{EndpointConfig, RdfWeatherDataset, Dataset, RdfDataset}
import supplychain.model.{Connection, Message, Supplier}
import supplychain.simulator.network.Network

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * The supply chain simulator.
 */
class Simulator(val actorSystem: ActorSystem, ec: EndpointConfig, productUri: String) extends Dataset {

  private val log = Logger.getLogger(classOf[Simulator].getName)

  // The current simulation date
  var currentDate = Scheduler.simulationStartDate //new DateTime(cal.getTimeInMillis)
  //DateTime.now

  // The simulation to run
  //private val sim = FairPhoneSimulation // CarSimulation

  // get the weather data provider
  //private val wd = new RdfWeatherDataset(ec.getEndpoint(), defaultWeatherGraph)
  private val wp = new WeatherProvider_(ec)

  // get the application configuration from endpoint
  private val cp = new ConfigurationProvider(ec, wp, productUri)
  val product = cp.getProduct()


  // Generate the supply chain network
  //private val network = Network.build(sim.product, wp, cp)
  private val network = Network.build(product, wp, cp)

  // Create actors for all suppliers
  for(supplier <- network.suppliers)
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
  private val dataset = new RdfDataset(ec)

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

  def step() {
    scheduler ! Scheduler.Tick
  }

  def run(frequency: Double) {
    stop()
    log.info("frequency: " + frequency)
    log.info("frequency.seconds: " + frequency.seconds);
    metronom = Option(actorSystem.scheduler.schedule(0 seconds, frequency.seconds, scheduler, Scheduler.Tick))
  }

  def stop() {
    metronom.foreach(_.cancel())
    metronom = None
  }

  def scheduleMessage(msg: Message) {
    scheduler ! msg
  }

  def getActor(supplier: Supplier) = {
    actorSystem.actorSelection("/user/" + supplier.id)
  }
}