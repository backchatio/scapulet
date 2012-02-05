package io.backchat.scapulet

import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffer
import akka.actor.ActorSystem

class XmppDecoder(implicit system: ActorSystem) extends SimpleChannelUpstreamHandler {

  val parser = new XmppPullParser()
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getMessage match {
      case buffer: ChannelBuffer if buffer.readable() ⇒ parser.parse(buffer)
      case _: ChannelBuffer ⇒ // this is not for us
      case x ⇒ ctx.sendUpstream(e)
    }
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    parser.stop
  }

  override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    parser.stop
  }
}
