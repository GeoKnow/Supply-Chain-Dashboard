package supplychain.simulator

import akka.actor.Actor
import supplychain.model._
import supplychain.simulator.Scheduler.Tick

import scala.collection.mutable

class Scheduler(rootConnection: Connection, simulator: Simulator) extends Actor {

  private var currentDate = DateTime.now

  private val tickInterval = Duration.days(1)

  private val orderInterval = Duration.days(30)

  private val orderCount = 10

  private var lastOrderTime = currentDate - orderInterval

  private val messageQueue = mutable.Queue[Message]()

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
      while(messageQueue.nonEmpty && messageQueue.front.date <= currentDate) {
        val msg = messageQueue.dequeue()
        val receiverActor = simulator.getActor(msg.receiver)
        receiverActor ! msg
      }
  }
}

object Scheduler {
  object Tick
}