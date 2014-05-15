package supplychain.simulation

import com.hp.hpl.jena.query.ResultSet
import supplychain.dataset.Dataset
import supplychain.model.{Connection, Supplier, Message}

class SimulatorDataset() extends Dataset {

  var messages = Seq[Message]()

  Simulation.addListener(msg => {
    messages = messages :+ msg
  })

  def suppliers: Seq[Supplier] = Simulation.suppliers

  def connections: Seq[Connection] = Simulation.connections

  override def addListener(listener: Message => Unit) {
    Simulation.addListener(listener)
  }

  def query(queryStr: String): ResultSet = throw new UnsupportedOperationException()
}
