package supplychain.simulator

import java.util.GregorianCalendar

import akka.actor.Actor
import supplychain.model._
import supplychain.simulator.Scheduler.Tick

import scala.collection.mutable

class Scheduler(rootConnection: Connection, simulator: Simulator) extends Actor {

  private val cal = new GregorianCalendar()
  cal.set(2010, 0, 1, 0, 0, 0);

  private val lastOrderCal = new GregorianCalendar()
  lastOrderCal.set(2013, 11, 31, 23, 59, 59)

  private val lastOrderDate = new DateTime(lastOrderCal.getTimeInMillis)

  // The current simulation date
  private var currentDate = new DateTime(cal.getTimeInMillis)
  //DateTime.now

  // The simulation interval between two ticks
  private val tickInterval = Duration.days(0.25)

  // The interval between two orders to the root supplier
  private val orderInterval = Duration.days(1)

  // The number of parts to be ordered
  private val orderCount = 10

  // Remembers the last order time
  private var lastOrderTime = currentDate - orderInterval

  // Scheduled messages ordered by date
  private val messageQueue = mutable.PriorityQueue[Message]()(Ordering.by(-_.date.milliseconds))

  /**
   * Receives and processes messages.
   */
  def receive = {
    case msg: Message => messageQueue.enqueue(msg)
    case Tick =>
      // Advance current date
      currentDate += tickInterval
      // Order
      if(lastOrderTime + orderInterval <= currentDate && currentDate <= lastOrderDate) {
        lastOrderTime = currentDate
        val rootActor = simulator.getActor(rootConnection.source)
        val order = Order(date = currentDate, connection = rootConnection, count = orderCount)
        rootActor ! order
      }
      // Send all due messages
      while(messageQueue.nonEmpty && messageQueue.head.date <= currentDate) {
        val msg = messageQueue.dequeue()
        val receiverActor = simulator.getActor(msg.receiver)
        receiverActor ! msg
      }
  }
}

object Scheduler {
  object Tick
}