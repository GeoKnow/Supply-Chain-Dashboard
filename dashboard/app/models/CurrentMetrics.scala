package models

import supplychain.metric.{Metric, Metrics, SilkMetrics}

/**
 * Holds the list of current metrics.
 */
object CurrentMetrics {

  @volatile
  private var metrics = List[Metric]()

  /* (Re)Loads all metrics. */
  def load() = {
    metrics = Metrics.all ++ SilkMetrics.load(Configuration.get.silkProject)
  }

  /* Retrieves the current metrics. */
  def apply() = {
    if(metrics.isEmpty)
      load()
    metrics
  }
}