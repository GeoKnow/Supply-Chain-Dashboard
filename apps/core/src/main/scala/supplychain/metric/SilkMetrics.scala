package supplychain.metric

import de.fuberlin.wiwiss.silk.execution.EvaluateTransform
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.workspace.{User, Project}
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask
import supplychain.model.Message

object SilkMetrics {

  // Register all default Silk plugins
  Plugins.register()

  /**
   * Loads Silk Metrics from Workspace.
   */
  def load(projectName: String) = {
    User().workspace.reload()

    User().workspace.projects.map(_.name.toString).foreach(println)
    println(projectName)

    for(project <- User().workspace.projects if project.name.toString == projectName;
        task <- project.transformModule.tasks;
        rule <- task.rules) yield {
      new SilkMetric(rule, task, project)
    }
  }.toList
}

class SilkMetric(rule: TransformRule, task: TransformTask, project: Project) extends Metric {
  /** The dimension that is measured, e.g., ''average production time'' */
  override def dimension: String = rule.name

  /** The unit of the returned values, e.g., ''seconds'' */
  override def unit: String = ""

  /** Computes this metric for a specific network. */
  override def apply(messages: Seq[Message]): Double = {
    try {
      val excuteTransform =
        new EvaluateTransform(
          source = project.sourceModule.task(task.dataset.sourceId).source,
          dataset = task.dataset,
          rules = Seq(rule)
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