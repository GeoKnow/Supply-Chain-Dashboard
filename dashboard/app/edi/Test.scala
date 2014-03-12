package edi

object Test extends App {

  val parser = new MessageParser()

  val messages = parser.read("C:\\Users\\RobertIsele\\Desktop\\DFUE\\empfang\\E130910.#C")

  for(message <- messages) {
    println(message)
  }
}
