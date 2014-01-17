package edi

import scala.io.Source

class MessageParser {

  // Message definitions
  private val messageDefs = DefinitionParser.load

  // Lookup table for message definitions
  private val messageMap = messageDefs.map(m => (m.id, m)).toMap

  /**
   * Reads a message from a file.
   */
  def read(msgFile: String): List[Message] = {
    val lines = Source.fromFile(msgFile).getLines().toList
    for(line <- lines if !line.trim.isEmpty) yield {
      readMessage(line)
    }
  }

  /**
   * Reads a single message line.
   */
  private def readMessage(line: String): Message = {
    // Read id
    val messageId = line.take(3).toInt
    // Retrieve message definition
    val messageDef = messageMap(messageId)
    // Read all fields
    val values = for(fieldDef <- messageDef.fields) yield line.substring(fieldDef.v - 1, fieldDef.b)
    // Build message
    Message(values, messageDef)
  }
}
