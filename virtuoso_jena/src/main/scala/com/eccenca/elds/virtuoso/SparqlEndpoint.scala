package com.eccenca.elds.virtuoso

import java.io.File
import java.util.logging.Logger

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.util.FileManager
import org.apache.jena.riot.{RDFLanguages, Lang, RDFDataMgr}
import virtuoso.jena.driver.{VirtuosoUpdateRequest, VirtGraph, VirtuosoQueryExecutionFactory, VirtDataset, VirtModel}
import com.hp.hpl.jena.query.ResultSet

import scala.collection.JavaConversions._

class SparqlEndpoint (virtuosoHost: String, virtuosoPort:String, virtuosoUser: String, virtuosoPassword: String) {

  private val logger = Logger.getLogger(getClass.getName)

  private val jdbcConnString = "jdbc:virtuoso://" + virtuosoHost + ":" + virtuosoPort + "/charset=UTF-8/log_enable=2"

  def select(query: String): ResultSet = {
    val virtGraph = new VirtGraph(jdbcConnString, virtuosoUser, virtuosoPassword)
    virtGraph.setReadFromAllGraphs(true)
    val queryExecution = VirtuosoQueryExecutionFactory.create(query, virtGraph)
    val result = queryExecution.execSelect()
    // close kills the ResultSet, so do not close
    //virtGraph.close()
    logger.info(s"Query issued:$query")
    result
  }

  def describe(query: String): Model = {
    val virtGraph = new VirtGraph(jdbcConnString, virtuosoUser, virtuosoPassword)
    virtGraph.setReadFromAllGraphs(true)
    val queryExecution = VirtuosoQueryExecutionFactory.create(query, virtGraph)
    val result = queryExecution.execDescribe()
    virtGraph.close()
    logger.info(s"Describe Query issued:$query")
    result
  }

  def update(query: String) {
    val virtGraph = new VirtGraph(jdbcConnString, virtuosoUser, virtuosoPassword)
    val request = new VirtuosoUpdateRequest(query, virtGraph)
    request.exec()
    logger.info(s"Update Query issued:$query")
    virtGraph.close()
  }

  def listGraphs(): Seq[String] = {
    val result = select("SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o }}").toList
    result.map(_.getResource("g")).filter(_ != null).map(_.getURI) // filter to remove unnamed graphs
  }

  def uploadDataset(graph: String, file: File, lang: Option[Lang]=None, clear: Boolean=false) = {
    createGraph(graph, clear)
    val virtGraph = new VirtGraph(graph, jdbcConnString, virtuosoUser, virtuosoPassword)
    val virtModel = new VirtModel(virtGraph)
    val m = FileManager.get().loadModel( file.getAbsolutePath )

    logger.info(s"Uploading dataset into graph < $graph > ...")

    //virtGraph.getBulkUpdateHandler.add(m.getGraph)

    virtModel.add(m)

    //val rdfLang = lang.getOrElse(RDFLanguages.filenameToLang(file.getName))
    //RDFDataMgr.read(virtGraph, file.getAbsolutePath, rdfLang)

    virtModel.close()
    virtGraph.close()
    logger.info(s"Uploaded dataset!")
  }

  /**
   * Creates a new graph.
   * @param graphUri The graph uri
   * @param clear If true, the graph is cleared before creating the new one.
   */
  def createGraph(graphUri: String, clear: Boolean): Unit = {
    val dropQuery = s"DROP SILENT GRAPH <$graphUri>"
    val createQuery = s"CREATE SILENT GRAPH <$graphUri>"
    if(clear) {
      logger.info(s"drop graph < $graphUri > ...")
      update(dropQuery)
    }
    logger.info(s"create graph < $graphUri > ...")
    update(createQuery)
  }
}