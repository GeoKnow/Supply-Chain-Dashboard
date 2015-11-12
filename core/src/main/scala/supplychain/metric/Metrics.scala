package supplychain.metric

import de.fuberlin.wiwiss.silk.workspace.{Project, User, Workspace}
import de.fuberlin.wiwiss.silk.execution.EvaluateTransform
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformTask
import supplychain.model.Message
import de.fuberlin.wiwiss.silk.plugins.Plugins

object Metrics {
  val all = new AverageDeliveryTime() :: new Timeliness() :: new AverageDelay() :: new DueParts() :: new LateOrders() :: Nil
}