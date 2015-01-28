package supplychain.model

import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.io.{Codec, Source}

/**
 * Created by rene on 21.01.15.
 */
case class XybermotiveData(supplierId: String, data: Seq[XybermotiveInventory], date: DateTime = DateTime.now) {

  def filter(searchStr: String) = {
    copy(data=data.filter(_.partNumber.contains(searchStr)))
  }
}

object XybermotiveData {

  private var cache = Map[String, XybermotiveData]()

  implicit val xybermotiveDataWrites: Writes[XybermotiveData] = (
    (JsPath \ "supplierId").write[String] and
      (JsPath \ "data").write[Seq[XybermotiveInventory]] and
      (JsPath \ "date").write[DateTime]
    )(unlift(XybermotiveData.unapply))

  def retrieve(supplierId: String) = {
    var id = "1"
    if (supplierId.trim().toLowerCase().startsWith("optic")) id = "1"
    if (supplierId.trim().toLowerCase().startsWith("pls")) id = "2"
    if (supplierId.trim().toLowerCase().startsWith("getrag")) id = "3"
    if (supplierId.trim().toLowerCase().startsWith("allgaier")) id = "4"

    val url = new java.net.URL("http://217.24.49.173/bestand_" + id + ".txt")
    println(url.toString)
    val conn = url.openConnection()
    val stream = conn.getInputStream

    val source = Source.fromInputStream(stream)(Codec.ISO8859)

    val lineList = source.getLines().toList.drop(1).map(_.trim).filter(!_.isEmpty)

    if (lineList.length > 0) {

      val xyData = XybermotiveData(supplierId, lineList.map(XybermotiveInventory.parseLine))

      source.close()

      cache += (supplierId -> xyData)

      val json = Json.toJson(xyData)
      //println(json.toString())
    }

    cache.getOrElse(supplierId, XybermotiveData(supplierId, Nil))
  }

  def retrieveJson(supplierId: String) = Json.toJson(retrieve(supplierId))

  def getAllInventory(supplier: Seq[Supplier]): Traversable[XybermotiveData] = {
    for (s <- supplier) {
      retrieve(s.id)
    }
    cache.values
  }
}


case class XybermotiveInventory(partNumber: String, partName: String, availableInventory: Float, freeInventory: Float) {
}

object XybermotiveInventory {

  implicit val xybermotiveInventoryWrites: Writes[XybermotiveInventory] = (
    (JsPath \ "partNumber").write[String] and
      (JsPath \ "partName").write[String] and
      (JsPath \ "availableInventory").write[Float] and
      (JsPath \ "freeInventory").write[Float]
    )(unlift(XybermotiveInventory.unapply))

  def parseLine(line: String): XybermotiveInventory = {
    val reg = "\"([^\"]*)\",\"([^\"]*)\",([0-9.-]*),([0-9.-]*)"
    //println(reg)
    //println(line)
    val regr = reg.r
    var xy: XybermotiveInventory = null

    line match {
      case regr(pn, pm, ai, fi) => {
        xy = XybermotiveInventory(pn, pm, ai.toFloat, fi.toFloat)
      }
    }
    xy
  }
}