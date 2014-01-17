package edi

import scala.util.parsing.combinator.RegexParsers
import java.io.{InputStreamReader, BufferedInputStream, Reader}
import java.nio.charset.Charset

/**
 * Parser for VDA message definitions.
 */
private class DefinitionParser() extends RegexParsers {

  /**
   * Parses a list of message definitions.
   */
  def parse(reader: Reader): List[MessageDef] = {
    parseAll(spec, reader) match {
      case Success(messages, _) => messages
      case error: NoSuccess => throw new RuntimeException(error.toString)
    }
  }

  // A specification contains a number of message definitions
  def spec = sep ~> repsep(msg, sep)

  // Separator between messages. Parses until '[' indicates the beginning of the next message
  def sep = "[^\\[]*".r

  // A message definition consists of a header and its fields
  def msg = header ~ rep(field) ^^ { case id ~ fields => MessageDef(id, fields) }

  // Message header, e.g. '[512]'
  def header = "[" ~> "\\d*".r <~ "]" ^^ { case id => id.toInt }

  // Each message consists of a number of fields of the format: POS|Satzart|KM|LG|AN|V|B|Pruefung|Beschreibung
  def field = "\\d+".r ~> p ~ p ~ p ~ p ~ p ~ p ~ p ~ p ^^ {
    case label ~ km ~ lg ~ an ~ v ~ b ~ pruefung ~ description =>
      FieldDef(label, km, lg, an, v.toInt, b.toInt, pruefung, description)
  }

  // Each field is described with a number of properties
  def p = "|" ~> "[^\\|\n]+".r
}

/**
 * Parser for VDA message definitions.
 */
object DefinitionParser {

  /**
   * Loads all message definitions.
   */
  def load = {
    val stream = getClass.getClassLoader.getResourceAsStream("vdaMessageDefinitions")
    val reader = new InputStreamReader(new BufferedInputStream(stream), "ISO-8859-1")
    new DefinitionParser().parse(reader)
  }
}