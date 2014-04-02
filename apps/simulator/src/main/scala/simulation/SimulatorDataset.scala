package simulation

import com.hp.hpl.jena.query.ResultSet
import dataset._
import dataset.Connection
import dataset.Supplier

object SimulatorDataset extends Dataset {

  private val simulation = new Simulation

  private var shippingsSeq = Seq[Shipping]()

  simulation.addListener(shipping => {
    shippingsSeq = shippingsSeq :+ shipping
  })

  def suppliers: Seq[Supplier] = simulation.suppliers

  def deliveries: Seq[Connection] = simulation.connections

  override def addListener(listener: Shipping => Unit) {
    simulation.addListener(listener)
  }

  def query(queryStr: String): ResultSet = throw new UnsupportedOperationException()


}
