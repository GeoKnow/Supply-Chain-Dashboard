package edi

case class Message(values: List[String], messageDef: MessageDef) {

  override def toString = {
    val valueStrings = for((field, value) <- messageDef.fields zip values) yield field.label + ": " + value
    valueStrings.mkString(s"[${messageDef.id}]\n", "\n", "\n")
  }
}
