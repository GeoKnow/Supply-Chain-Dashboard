package supplychain.model

/**
 * Created by rene on 21.01.15.
 */
case class XybermotiveData(data: List[XybermotiveInventory]) {

}


case class XybermotiveInventory(partNumber: String, partName: String, availableInventory: Integer, freeInventory: Integer) {

}

object XybermotiveInventory {

  def parseLine(line: String): XybermotiveInventory = {
    println(line.split("\"?,\"?").toList)
    val Array(partNo, partName, availInv, freeInv) = line.substring(1).split("\"?,\"?")
    XybermotiveInventory(partNo, partName, availInv.toInt, freeInv.toInt)
  }
}