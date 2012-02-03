package io.backchat.scapulet

import java.io._
import java.net.{ Socket }
import xml._
import StringUtil._
import XMPPConstants._
import akka.actor.{ ActorSystem, ActorRef }
import io.backchat.scapulet.Scapulet.{ ScapuletConnection, ClientConfig }

object ClientConnection {

  object StreamResponse {
    private val streamRegex = """(<\?[^?]>)?<stream:stream[^>]*>""".r

    def unapply(msg: String) = streamRegex.findFirstMatchIn(msg) match {
      case Some(start) =>
        val x = XML.loadString(start + "</stream:stream>")
        Some(((x \ "@id").text, (x \ "@from").text))
      case _ => None
    }
  }

  object AuthenticationFailureResponse {
    private val regex = "(<stream:error>.*</stream:error>)(.*)".r

    def unapply(msg: String) = regex.findFirstMatchIn(msg) match {
      case Some(m) => {
        val x = XML.loadString(m.group(1))
        Some((x \ "text").text)
      }
      case _ => None
    }
  }

  class SocketConnection(connectionConfig: ClientConfig)(implicit protected val system: ActorSystem) extends ScapuletConnection with Logging {

    import connectionConfig._
    import concurrent.ops._

    private var _out: Writer = _
    private var _in: InputStream = _
    private var _connection: Socket = _
    private var _shutdown = false

    private var _xmlProcessorOption: Option[ActorRef] = None

    def xmlProcessor = _xmlProcessorOption

    def xmlProcessor_=(processor: ActorRef) = _xmlProcessorOption = Option(processor)

    val serverAddress = socketAddress

    val address = connectionConfig.address

    def hexCredentials(id: String) = asHexSecret(id)

    def connect = {
      _connection = new Socket
      _connection.connect(serverAddress, connectionConfig.connectionConfig.connectionTimeout.toMillis.toInt)
      _out = new BufferedWriter(new OutputStreamWriter(_connection.getOutputStream, Utf8))
      _in = _connection.getInputStream
      spawn {
        _out write generateStreamStart(Some(domain))
        while (!_shutdown) {
          read
        }
      }

    }

    def read {
      val reader = new BufferedReader(new InputStreamReader(_in, Utf8))
      //val reader = new XMPPStreamReader(_in)
      var line = reader.readLine
      while (line != null) {
        line match {
          case StreamResponse(id, from) => {

          }
          case _ => {

            val stanza = loadXml(line)
            _xmlProcessorOption foreach {
              _ ! stanza
            }
          }
        }
        line = reader.readLine
      }
    }

    private def loadXml(line: String) = {
      try {
        List(XML.loadString(line))
      } catch {
        case e: SAXParseException => {
          val doc = XML.loadString("<wrapper>%s</wrapper>".format(line))
          doc.child.toList
        }
      }
    }

    private def generateStreamStart(
      to: Option[String] = None,
      from: Option[String] = None,
      id: Option[String] = None,
      xmlLang: Option[String] = Some("en"),
      xmlns: Option[String] = Some(CLIENT_NS),
      version: Option[String] = Some("1.0")) = {
      val sb = new StringBuffer
      sb.append("""<stream:stream xmlns:stream="http://etherx.jabber.org/streams" """)
      xmlns foreach {
        s => sb.append("xmlns=\"%s\" " format s)
      }
      to foreach {
        s => sb.append("to=\"%s\" " format s)
      }
      from foreach {
        s => sb.append("from=\"%s\" " format s)
      }
      id foreach {
        s => sb.append("id=\"%s\" " format s)
      }
      xmlLang foreach {
        s => sb.append("xml:lang=\"%s\" " format s)
      }
      version foreach {
        s => sb.append("version=\"%s\" " format s)
      }
      sb append ">"
      sb.toString
    }

    def write(nodes: Seq[Node]) = {
      _out write nodes.map(xml.Utility.trimProper _).toString
      _out.flush
    }

    def disconnect = {
      _shutdown = true

      // Log but swallow all errors while closing the connection
      try {
        _connection.close
      } catch {
        case e => logger.error(e, "An error occurred while closing the connection")
      }
      val (host, port) = (connectionConfig.connectionConfig.host, connectionConfig.connectionConfig.port)
      logger info "XMPP component [%s] disconnected from host [%s:%d].".format(address, host, port)
      _connection = null
    }

  }

}

