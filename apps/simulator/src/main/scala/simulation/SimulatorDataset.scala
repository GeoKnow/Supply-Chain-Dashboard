package simulation

import com.hp.hpl.jena.query.ResultSet
import dataset._
import dataset.Connection
import dataset.Supplier

object SimulatorDataset extends Dataset {

  private val simulation = new Simulation

  private var messages = Seq[Message]()

  simulation.addListener(msg => {
    messages = messages :+ msg
  })

  def suppliers: Seq[Supplier] = simulation.suppliers

  def deliveries: Seq[Connection] = simulation.connections

  override def addListener(listener: Message => Unit) {
    simulation.addListener(listener)
  }

  def query(queryStr: String): ResultSet = throw new UnsupportedOperationException()


}
