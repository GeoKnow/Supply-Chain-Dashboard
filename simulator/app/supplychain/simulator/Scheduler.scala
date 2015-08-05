package supplychain.simulator

import akka.actor.Actor
import play.api.Logger
import supplychain.model._
import supplychain.simulator.Scheduler.Tick
import scala.collection.mutable

class Scheduler(rootConnection: Connection, simulator: Simulator) extends Actor {

  def currentDate = simulator.currentDate
  def simulationEndDate = simulator.simulationEndDate

  // The simulation interval between two ticks
  private val tickInterval = Duration.days(Configuration.get.tickIntervalsDays)

  // The interval between two orders to the root supplier
  private val orderInterval = Duration.days(Configuration.get.orderIntervalDays)

  // The number of parts to be ordered
  private val orderCount = Configuration.get.orderCount // 10 // + (Random.nextDouble() * 10.0).toInt

  // Remembers the last order time
  @volatile
  private var lastOrderTime: DateTime = currentDate - orderInterval

  // Scheduled messages ordered by date
  private val messageQueue = mutable.PriorityQueue[Message]()(Ordering.by(-_.date.milliseconds))

  /**
   * Receives and processes messages.
   */
  def receive = {
    case msg: Message => messageQueue.enqueue(msg)
    case Tick =>
      // Advance current date
      simulator.currentDate += tickInterval
      Logger.info(s"current simulation date: $currentDate")
      // Order
      if(lastOrderTime + orderInterval <= currentDate && currentDate <= simulationEndDate) {
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