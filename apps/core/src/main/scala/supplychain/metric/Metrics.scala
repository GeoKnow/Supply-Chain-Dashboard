package supplychain.metric

import de.fuberlin.wiwiss.silk.workspace.{Project, User, Workspace}
import de.fuberlin.wiwiss.silk.execution.EvaluateTransform
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask
import supplychain.model.Message
import de.fuberlin.wiwiss.silk.plugins.Plugins

object Metrics {

  Plugins.register()

  val buildIn = new AverageProductionTime() :: new Timeliness() :: new AverageDelay() :: new DueOrders() :: Nil

  var all = buildIn ++ silkMetrics

  def reload() = {
    User().workspace.reload()
    all = buildIn ++ silkMetrics
  }

  def silkMetrics = {
    for(project <- User().workspace.projects;
        task <- project.transformModule.tasks) yield {
      new SilkMetric(task, project)
    }
  }
}

class SilkMetric(task: TransformTask, project: Project) extends Metric {
  /** The dimension that is measured, e.g., ''average production time'' */
  override def dimension: String = task.name

  /** The unit of the returned values, e.g., ''seconds'' */
  override def unit: String = ""

  /** Computes this metric for a specific network. */
  override def apply(messages: Seq[Message]): Double = {
    try {
      val excuteTransform =
        new EvaluateTransform(
          source = project.sourceModule.task(task.dataset.sourceId).source,
          dataset = task.dataset,
          rules = Seq(task.rule)
        )

      val entities = excuteTransform()

      val avg = entities.map(_.values.head.values.head.toDouble).sum / entities.size

      avg
    } catch {
      case ex: Exception => 0.0
      case ex: RuntimeException => 0.0
    }

  }

}
