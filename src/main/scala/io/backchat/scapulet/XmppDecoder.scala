package io.backchat.scapulet

import _root_.org.jboss.netty.channel._
import _root_.org.jboss.netty.buffer.ChannelBuffer
import akka.actor.ActorSystem
import util.control.Exception._
import xml._

class XmppDecoder(implicit system: ActorSystem) extends SimpleChannelUpstreamHandler {

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case buffer: ChannelBuffer if buffer.readable() ⇒ {
        buffer.toString(Utf8) match {
          case source ⇒ {
            (catching(classOf[SAXParseException]) withApply wrap(source)_) {
              List(XML.loadString(source))
            } foreach system.eventStream.publish
          }
        }
      }
      case _: ChannelBuffer ⇒ // this is not for us
      case x                ⇒ ctx.sendUpstream(e)
    }
  }

  private def wrap(source: String)(th: Throwable) = XML.loadString("<wrapper>%s</wrapper>".format(source)).child.toList

}
