package simulation

import com.hp.hpl.jena.query.ResultSet
import dataset.{Address, Delivery, Dataset}

object SimulatorDataset extends Dataset {

  private val simulation = new Simulation

  private var deliveriesSeq = Seq[Delivery]()

  simulation.addListener(delivery => {
    deliveriesSeq = deliveriesSeq :+ delivery
  })

  def addresses: Seq[Address] = simulation.suppliers

  def deliveries: Seq[Delivery] = deliveriesSeq

  override def addListener(listener: Delivery => Unit) {
    simulation.addListener(listener)
  }

  def query(queryStr: String): ResultSet = throw new UnsupportedOperationException()


}
