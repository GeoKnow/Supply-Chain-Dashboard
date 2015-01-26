package supplychain.model

/**
 * Created by rene on 21.01.15.
 */
case class XybermotiveData(data: List[XybermotiveInventory]) {

}


case class XybermotiveInventory(partNumber: String, partName: String, availableInventory: Float, freeInventory: Float) {

}

object XybermotiveInventory {

  def parseLine(line: String): XybermotiveInventory = {
    //println(line.split("\"?,\"?").toList)
    val reg = "\"([^\"]*)\",\"([^\"]*)\",([0-9.-]*),([0-9.-]*)"
    println(reg)
    println(line)

    val regr = reg.r
    var xy: XybermotiveInventory = null

    line match {
      case regr(pn, pm, ai, fi) => {
        println("pn: " + pn)
        println("pm: " + pm)
        println("ai: " + ai)
        println("fi: " + fi)
        xy = XybermotiveInventory(pn, pm, ai.toFloat, fi.toFloat)
      }
    }

    xy
  }
}