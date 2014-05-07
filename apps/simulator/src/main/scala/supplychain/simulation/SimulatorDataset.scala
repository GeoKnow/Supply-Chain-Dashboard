package supplychain.simulation

import com.hp.hpl.jena.query.ResultSet
import supplychain.dataset.Dataset
import supplychain.model.{Connection, Supplier, Message}

class SimulatorDataset() extends Dataset {

  private val simulation = new Simulation

  var messages = Seq[Message]()

  simulation.addListener(msg => {
    messages = messages :+ msg
  })

  def suppliers: Seq[Supplier] = simulation.suppliers

  def connections: Seq[Connection] = simulation.connections

  override def addListener(listener: Message => Unit) {
    simulation.addListener(listener)
  }

  def query(queryStr: String): ResultSet = throw new UnsupportedOperationException()


}
