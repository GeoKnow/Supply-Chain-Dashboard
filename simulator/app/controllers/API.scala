package controllers

import com.hp.hpl.jena.query.ResultSetFormatter

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import supplychain.dataset.{RdfDataset, MetricsDataset}
import supplychain.model.DateTime
import supplychain.simulator
import supplychain.simulator.exceptions.SimulationPeriodOutOfBoundsException
import supplychain.simulator.{Configuration, Simulator}
import supplychain.exceptions.UnknownProductException

/**
 * The REST API.
 */
object API extends Controller {

  private val logger = Logger(this.getClass)

  private var metricsCalculating = false
  private var simulationRunning = false

  /**
   * Issues a SPARQL select query.
   * @param query The query
   * @return A html page, if the ACCEPT header includes html.
   *         Otherwise, SPARQL results XML.
   */
  def sparql(query: String) = Action { implicit request =>
    logger.debug("Received query:\n" + query)
    val resultSet = Simulator().query(query)
    val resultXML = ResultSetFormatter.asXMLString(resultSet)

    render {
      case _ => {
        Ok(resultXML).as("application/sparql-results+xml")
      }
    }
  }

  def run(start: Option[String], end: Option[String], productUri: Option[String], graphUri: Option[String], interval: Double) = Action {
    if (simulationRunning || Simulator.isSimulationRunning()) {
      ServiceUnavailable("Simulation is running. Retry later.")
    } else {
      logger.info(s"Simulation started at '$start' and will run until '$end' with an interval of '$interval' seconds.")

      //clear graph
      Configuration.get.endpointConfig.getEndpoint().createGraph(Configuration.get.endpointConfig.getDefaultGraph(), true)

      simulationRunning = true
      val s = start.map(DateTime.parse)
      val e = end.map(DateTime.parse)

      try {
        Simulator.run(interval, s, e, productUri, graphUri)
        simulationRunning = false
        Ok("run")
      } catch {
        case e1: UnknownProductException => BadRequest(e1.message)
        case e2: SimulationPeriodOutOfBoundsException => BadRequest(e2.message)
        case e3: Exception => BadRequest(e3.getMessage)
      } finally {
        simulationRunning = false
      }
    }
  }

  def pause() = Action {
    logger.info(s"Simulation paused.")
    Simulator().pause()
    Ok("pause")
  }

  def status() = Action {
    logger.info(s"Simulation status.")
    var status = "Ready for simulation."
    if (simulationRunning || Simulator.isSimulationRunning()) status = "Simulation is running."
    else if (metricsCalculating) status = "Metrics calculation is running."
    var startDate = "None"
    var endDate = "None"
    var curDate ="None"

    try {
      startDate = Simulator().startDate.toXSDFormat
      endDate = Simulator().simulationEndDate.toXSDFormat
      curDate = Simulator().currentDate.toXSDFormat
    } catch {
      case e1: NullPointerException =>
    }

    val statusJson = Json.obj(
      "status" -> status,
      "simulationStartDate" -> startDate,
      "simulationEndDate" -> endDate,
      "simulationCurrentDate" -> curDate,
      "configuration" -> Configuration.get
    )
    Ok(statusJson)
  }

  def calculateMetrics(productUri: Option[String], graphUri: Option[String]) = Action {
    if (!Simulator.isSimulationRunning()) {
      metricsCalculating = true
      logger.info(s"Calculate performance metrics.")

      //clear graph
      Configuration.get.endpointConfig.getEndpoint().createGraph(Configuration.get.endpointConfig.getDefaultGraphMetrics(), true)

      for (p <- productUri) Configuration.get.productUri = p
      for (g <- graphUri) Configuration.get.endpointConfig.defaultGraph = g
      val md = new MetricsDataset(Configuration.get.endpointConfig, Configuration.get.silkProject, Configuration.get.minStartDate, Configuration.get.maxEndDate)
      md.generateDataSet()

      if (Simulator().messages.isEmpty) {
        logger.info("Simulation not runned before, loading messages first.")
        Simulator().loadMessages()
        logger.info("Loaded number of messages: " + Simulator().messages.size.toString)
      }

      var currentDate = Simulator().startDate
      while(currentDate <= Simulator().simulationEndDate) {
        logger.debug("Simulator().messages.size: " + Simulator().messages.size.toString)
        for (s <- Simulator().network.suppliers) {
          val messages = Simulator().messages.filter(_.date <= currentDate).filter(_.connection.source.id == s.id)
          md.addMetricValue(messages, s, currentDate)
        }
        currentDate += Simulator().tickInterval
      }
      md.normalizeDataCube()
      metricsCalculating = false
      Ok("metrics")
    } else {
      ServiceUnavailable("Simulation is running, can not calculate performance metrics now. Retry later.")
    }
  }
}
