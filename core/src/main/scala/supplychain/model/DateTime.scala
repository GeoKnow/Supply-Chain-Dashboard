package supplychain.model

import java.text.SimpleDateFormat
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import java.util.{Calendar, GregorianCalendar}

/**
 * Represents a date time.
 */
case class DateTime(milliseconds: Long) extends Ordered[DateTime] {

  def +(duration: Duration) = {
    new DateTime(milliseconds + duration.milliseconds)
  }

  def -(duration: Duration) = {
    new DateTime(milliseconds - duration.milliseconds)
  }

  def -(date: DateTime) = {
    Duration.milliseconds(milliseconds - date.milliseconds)
  }

  def compare(d: DateTime) = milliseconds.compare(d.milliseconds)

  def toXSDFormat = {
    val calendar = new GregorianCalendar()
    calendar.setTimeInMillis(milliseconds)
    DateTime.datetypeFactory.newXMLGregorianCalendar(calendar).toXMLFormat
  }

  def toFormat(format: String): String = {
    val calendar = new GregorianCalendar()
    calendar.setTimeInMillis(milliseconds)
    val formatted = new SimpleDateFormat(format)
    return formatted.format(calendar.getTime)
  }

  override def toString = toXSDFormat

  def getMonth(): Int = {
    val gcal = new GregorianCalendar()
    gcal.setTimeInMillis(milliseconds)
    return gcal.get(Calendar.MONTH)
  }

  def getYear(): Int = {
    val gcal = new GregorianCalendar()
    gcal.setTimeInMillis(milliseconds)
    return gcal.get(Calendar.YEAR)
  }
}

object DateTime {

  private val datetypeFactory = DatatypeFactory.newInstance()

  def now = new DateTime(System.currentTimeMillis())

  def parse(format: String, dateString: String): DateTime = {
    val formatted = new java.text.SimpleDateFormat(format)
    return new DateTime(formatted.parse(dateString).getTime)
  }
}
