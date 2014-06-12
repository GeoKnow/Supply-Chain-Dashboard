package supplychain.simulator

import akka.actor.Actor
import supplychain.model._
import supplychain.simulator.Scheduler.Tick

import scala.collection.mutable

class Scheduler(rootConnection: Connection, simulator: Simulator) extends Actor {

  // The current simulation date
  private var currentDate = DateTime.now

  // The simulation interval between two ticks
  private val tickInterval = Duration.days(1)

  // The interval between two orders to the root supplier
  private val orderInterval = Duration.days(30)

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
      if(lastOrderTime + orderInterval <= currentDate) {
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