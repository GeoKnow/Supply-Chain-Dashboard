package supplychain.metric

import supplychain.model.{DateTime, Message}

/**
 * A supply chain metric.
 */
trait Metric {

  /** The dimension that is measured, e.g., ''average production time'' */
  def dimension: String

  /** The unit of the returned values, e.g., ''seconds''*/
  def unit: String

  /** Computes this metric for a specific network. */
  def apply(messages: Seq[Message], currentDate: DateTime): Double
}