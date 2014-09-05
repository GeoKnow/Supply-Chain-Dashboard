package supplychain.model

import javax.xml.datatype.DatatypeFactory

/**
 * A time duration.
 */
case class Duration(milliseconds: Long) {

  def +(d: Duration) = new Duration(milliseconds + d.milliseconds)

  def <(d: Duration) = milliseconds < d.milliseconds

  def >(d: Duration) = milliseconds > d.milliseconds

  def <=(d:Duration) = milliseconds <= d.milliseconds

  def >=(d: Duration) = milliseconds >= d.milliseconds

  def ==(d: Duration) = milliseconds == d.milliseconds

  def *(scale: Double) = new Duration((milliseconds * scale).toLong)

  def /(scale: Double) = new Duration((milliseconds / scale).toLong)

  def *(scale: Int) = new Duration(milliseconds * scale)

  def /(scale: Int) = new Duration(milliseconds / scale)

  override def toString = Duration.datetypeFactory.newDuration(milliseconds).toString
}

/**
 * Factory for specifying time durations.
 */
object Duration {

  private val datetypeFactory = DatatypeFactory.newInstance()

  /* Specifies a duration in milliseconds. */
  def milliseconds(m: Long) = new Duration(m)

  /* Specifies a duration in days */
  def days(d: Double) = new Duration((d * 24 * 60 * 60 * 1000).toLong)
}
