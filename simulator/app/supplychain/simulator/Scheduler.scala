package supplychain.simulator

import akka.actor.Actor
import play.api.Logger
import supplychain.model._
import supplychain.simulator.Scheduler.Tick
import scala.collection.mutable

class Scheduler(rootConnection: Connection, simulator: Simulator) extends Actor {

  def simulationEndDate = simulator.simulationEndDate

  // The interval between two orders to the root supplier
  private val orderInterval = Duration.days(Configuration.get.orderIntervalDays)

  // The number of parts to be ordered
  private val orderCount = Configuration.get.orderCount // 10 // + (Random.nextDouble() * 10.0).toInt

  // Remembers the last order time
  @volatile
  private var lastOrderTime: DateTime = new DateTime(0)

  // Scheduled messages ordered by date
  private val messageQueue = mutable.PriorityQueue[Message]()(Ordering.by(-_.date.milliseconds))

  /**
   * Receives and processes messages.
   */
  def receive = {
    case msg: Message =>

      if (simulator.currentDate >= msg.date) simulator.getActor(msg.receiver) ! msg
      else messageQueue.enqueue(msg)

    case Tick(currentDate) =>

      //if (currentDate > simulationEndDate)
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
  case class Tick(currentDate: DateTime)
}