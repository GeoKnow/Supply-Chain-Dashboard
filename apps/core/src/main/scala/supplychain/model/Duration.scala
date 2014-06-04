package supplychain.model

import javax.xml.datatype.DatatypeFactory

class Duration(val milliseconds: Long) {

  override def toString = Duration.datetypeFactory.newDuration(milliseconds).toString

  def +(d: Duration) = new Duration(milliseconds + d.milliseconds)

  def *(scale: Double) = new Duration((milliseconds * scale).toLong)

  def /(scale: Double) = new Duration((milliseconds / scale).toLong)

  def *(scale: Int) = new Duration(milliseconds * scale)

  def /(scale: Int) = new Duration(milliseconds / scale)
}

object Duration {
  private val datetypeFactory = DatatypeFactory.newInstance()

  def milliseconds(m: Long) = new Duration(m)

  def days(d: Double) = new Duration((d * 24 * 60 * 60 * 1000).toLong)
}
