package supplychain.model

import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import java.util.GregorianCalendar

/**
 * Represents a date time.
 */
class DateTime(val milliseconds: Long) extends Ordered[DateTime] {

  def +(duration: Duration) = {
    new DateTime(milliseconds + duration.milliseconds)
  }

  def -(date: DateTime) = {
    Duration.milliseconds(milliseconds - date.milliseconds)
  }

  def compare(d: DateTime) = (milliseconds - d.milliseconds).toInt

  def toXSDFormat = {
    val calendar = new GregorianCalendar()
    calendar.setTimeInMillis(milliseconds)
    DateTime.datetypeFactory.newXMLGregorianCalendar(calendar).toXMLFormat
  }
}

object DateTime {

  private val datetypeFactory = DatatypeFactory.newInstance()

  def now = new DateTime(System.currentTimeMillis())
}
