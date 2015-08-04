package supplychain.metric

import supplychain.dataset.Dataset
import supplychain.model.{Message, Duration, DateTime}


object Evaluator {

  def table(dataset: Dataset, metrics: Seq[Metric], supplierId: String) = {
    val startDate = DateTime.now
    val endDate = DateTime.now + Duration.days(360)
    val interval = Duration.days(30)

    var scoreTable = Seq[Seq[Double]]()
    var currentMessages = Seq[Message]()
    var currentEndDate = startDate + interval
    for(m <- dataset.messages if m.connection.target.id == supplierId && m.date >= startDate && m.date <= endDate) {
      if(m.date >= currentEndDate) {
        currentEndDate += interval
        val scores = for(metric <- metrics) yield metric(currentMessages)
        scoreTable = scoreTable :+ scores
        currentMessages = List[Message]()
      }
      currentMessages :+ m
    }

    scoreTable
  }
}
