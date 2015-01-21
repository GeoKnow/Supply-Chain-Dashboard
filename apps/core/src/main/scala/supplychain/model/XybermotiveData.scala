package supplychain.model

/**
 * Created by rene on 21.01.15.
 */
case class XybermotiveData(data: List[XybermotiveInventory]) {

}


case class XybermotiveInventory(partNumber: String, partName: String, availableInventory: Integer, freeInventory: Integer) {

}