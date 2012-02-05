package io.backchat.scapulet

import org.jboss.netty.buffer.{ ChannelBuffers, ChannelBufferInputStream, ChannelBuffer }
import xml._
import org.codehaus.stax2.XMLInputFactory2
import collection.JavaConversions._
import akka.actor.ActorSystem
import java.util.concurrent.atomic.AtomicBoolean
import akka.dispatch.{ Await, Future }
import akka.util.duration._
import javax.xml.stream.{ XMLEventReader, XMLInputFactory }
import javax.xml.stream.events.{ XMLEvent, Namespace, EntityReference, Attribute ⇒ JAttribute }
import java.io.{ PipedInputStream, PipedOutputStream }

object XmppPullParser {
  private case class ParentRef(element: Elem, parent: Option[ParentRef] = None)
}
class XmppPullParser(implicit val system: ActorSystem) extends Logging {

  private val factory: XMLInputFactory2 = XMLInputFactory.newInstance().asInstanceOf[XMLInputFactory2]
  factory.setProperty(XMLInputFactory.IS_COALESCING, true)
  factory.setProperty(XMLInputFactory.IS_VALIDATING, false)
  factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true)
  factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
  factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true)

  import XmppPullParser._

  private val keepGoing = new AtomicBoolean(true)
  private implicit val executor = system.dispatcher
  private val outputStream = new PipedOutputStream()
  private val inputStream = new PipedInputStream(outputStream)

  executor.execute(new Runnable {
    def run() {
      val reader = factory.createXMLEventReader(inputStream)
      readStreamLoop(reader, null)
    }
  })

  def parse(buffer: ChannelBuffer) {
    Future {
      buffer.readBytes(outputStream, buffer.readableBytes())
    } onSuccess {
      case _ ⇒ logger debug ("offered a byte buffer")
    } onFailure {
      case e ⇒ logger error (e, "Couldn't offer a byte buffer for the queue")
    }
  }

  def stop {
    keepGoing.set(false)
    outputStream.close()
    inputStream.close()
  }

  private def readStreamLoop(reader: XMLEventReader, parent: ParentRef) = {
    var current = parent
    logger debug ("Handling stream loop now with a current value of:\n{}", current)
    logger debug ("This reader has an element? %s" format reader.hasNext)
    while (keepGoing.get() && reader.hasNext) {
      logger debug "entering loop"
      val event = reader.nextEvent()
      logger debug "The received event: %s".format(event)
      current = handleStartEvent(event, current)
      current = handleEndEvent(event, current)
      if (event.isCharacters) {
        current = addChild(current, Text(event.asCharacters().getData))
      }
      if (event.isEntityReference) { current = addChild(current, EntityRef(event.asInstanceOf[EntityReference].getName)) }

      logger debug "There is more to come? %s and there is".format(reader.hasNext)
    }
    current
  }

  private def handleStartEvent(event: XMLEvent, current: ParentRef) = {
    if (event.isStartElement) {
      logger debug ("Received a start element")
      val ele = event.asStartElement()
      val attributes = ele.getAttributes.foldLeft(Null.asInstanceOf[MetaData]) { (acc, attr) ⇒
        val att = attr.asInstanceOf[JAttribute]
        if (att.getName.getNamespaceURI.blank) {
          new UnprefixedAttribute(att.getName.getLocalPart, att.getValue, acc)
        } else {
          new PrefixedAttribute(att.getName.getPrefix, att.getName.getLocalPart, att.getValue, acc)
        }
      }
      val scope = ele.getNamespaces.foldLeft(TopScope.asInstanceOf[NamespaceBinding]) { (acc, nss) ⇒
        val namespace = nss.asInstanceOf[Namespace]
        NamespaceBinding(namespace.getPrefix.blankOpt.orNull, namespace.getNamespaceURI.blankOpt.orNull, acc)
      }
      ParentRef(Elem(ele.getName.getPrefix.blankOpt.orNull, ele.getName.getLocalPart, attributes, scope), Option(current))
    } else current
  }

  private def handleEndEvent(event: XMLEvent, current: ParentRef) = {
    if (event.isEndElement) {
      logger debug ("Received an end element")
      if (current.parent.isDefined) {
        logger debug ("from a child")
        addChild(current.parent.get, current.element)
      } else {
        logger debug "publishing element to event stream"
        system.scapulet.eventStream publish current.element
        null
      }
    } else current
  }

  private def addChild(nodeRef: ParentRef, childNode: Node) = {
    ParentRef(nodeRef.element.copy(child = nodeRef.element.child ++ childNode), nodeRef.parent)
  }

}
