package com.eccenca.elds.virtuoso

import java.io.File
import java.util.logging.Logger

import com.hp.hpl.jena.query.QuerySolution
import com.hp.hpl.jena.rdf.model.Model
import org.apache.jena.riot.{RDFLanguages, Lang, RDFDataMgr}
import virtuoso.jena.driver.{VirtuosoUpdateRequest, VirtGraph, VirtuosoQueryExecutionFactory}
import com.hp.hpl.jena.query.ResultSet

import scala.collection.JavaConversions._

class SparqlEndpoint (virtuosoHost: String, virtuosoPort:String, virtuosoUser: String, virtuosoPassword: String) {

  private val logger = Logger.getLogger(getClass.getName)

  private val jdbcConnString = "jdbc:virtuoso://" + virtuosoHost + ":" + virtuosoPort

  def select(query: String): ResultSet = {
    val virtGraph = new VirtGraph(jdbcConnString, virtuosoUser, virtuosoPassword)
    virtGraph.setReadFromAllGraphs(true)
    val queryExecution = VirtuosoQueryExecutionFactory.create(query, virtGraph)
    val result = queryExecution.execSelect()
    virtGraph.close()
    logger.info(s"Query issued:$query")
    result
  }

  def describe(query: String): Model = {
    val virtGraph = new VirtGraph(jdbcConnString, virtuosoUser, virtuosoPassword)
    virtGraph.setReadFromAllGraphs(true)
    val queryExecution = VirtuosoQueryExecutionFactory.create(query, virtGraph)
    val result = queryExecution.execDescribe()
    virtGraph.close()
    logger.info(s"Query issued:$query")
    result
  }

  def update(query: String) {
    val virtGraph = new VirtGraph(jdbcConnString, virtuosoUser, virtuosoPassword)
    val request = new VirtuosoUpdateRequest(query, virtGraph)
    request.exec()
    virtGraph.close()
  }

  def listGraphs(): Seq[String] = {
    val result = select("SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o }}").toList
    result.map(_.getResource("g")).filter(_ != null).map(_.getURI) // filter to remove unnamed graphs
  }

  def uploadDataset(graph: String, file: File, lang: Option[Lang]=None) = {
    createGraph(graph, clear = true)
    val virtGraph = new VirtGraph(graph, jdbcConnString, virtuosoUser, virtuosoPassword)
    logger.info(s"Uploading dataset into graph < $graph > ...")
    val rdfLang = lang.getOrElse(RDFLanguages.filenameToLang(file.getName))
    RDFDataMgr.read(virtGraph, file.getAbsolutePath, rdfLang)
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
    if(clear)
      update(s"$dropQuery $createQuery")
    else
      update(createQuery)
  }

}
