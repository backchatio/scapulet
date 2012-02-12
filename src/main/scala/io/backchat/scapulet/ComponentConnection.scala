package io.backchat.scapulet

import util.control.Exception._
import _root_.org.jboss.netty.channel._
import akka.actor._
import _root_.org.jboss.netty.buffer.{ ChannelBuffers, ChannelBuffer }
import xml._
import jivesoftware.openfire.nio.XMLLightweightParser
import io.backchat.scapulet.Stanza.Predicate

object ComponentConnection {

  private[scapulet] object OpenStream {
    val openStreamFormatString = """<stream:stream xmlns="%s" xmlns:stream="%s" to="%s" >"""
    def apply(jid: String, nsd: String = ns.component.Accept) = openStreamFormatString.format(nsd, ns.Stream, jid)

  }

  private[scapulet] object StreamResponse {
    private val streamRegex = """(<\?[^?]>)?<stream:stream[^>]*>""".r

    def unapply(msg: String) = streamRegex.findFirstMatchIn(msg) match {
      case Some(start) ⇒
        val x = XML.loadString(start + "</stream:stream>")
        Some(((x \ "@id" text), (x \ "@from" text)))
      case _ ⇒ None
    }
  }

  private[scapulet] object AuthenticationFailureResponse {
    private val regex = "(<stream:error>.*</stream:error>)(.*)".r

    def unapply(msg: String) = regex.findFirstMatchIn(msg) match {
      case Some(m) ⇒ {
        val x = XML.loadString(m.group(1))
        Some(x \ "text" text)
      }
      case _ ⇒ None
    }
  }

  private[scapulet] case class IncomingStanza(elem: Node)

  private[scapulet] class ComponentConnectionHandler(config: ConnectionConfig)(implicit protected val system: ActorSystem, actor: ActorRef) extends SimpleChannelUpstreamHandler with Logging {

    val parser = new XMLLightweightParser("UTF-8")

    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      logger info ("Connected to server, authenticating...")
      val buff = ChannelBuffers.copiedBuffer(OpenStream(config.address), Utf8)

      e.getChannel.write(buff)
    }

    override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      if (!actor.isTerminated) actor ! Scapulet.Disconnected
    }

    override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      if (!actor.isTerminated) {
        logger warning "XMPP connection to [%s:%s] has failed, trying to reconnect in %d seconds.".format(
          config.host, config.port, config.reconnectDelay.toSeconds)
        system.scheduler.scheduleOnce(config.reconnectDelay, actor, Scapulet.Reconnect)
      }
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      (allCatch withApply logError) {
        e.getMessage match {
          case b: ChannelBuffer if b.readable() ⇒ {
            parser.read(b.toByteBuffer)
            if (parser.areThereMsgs()) {
              parser.getMsgs foreach {
                case AuthenticationFailureResponse(error) ⇒ throw new UnauthorizedException(error)
                case StreamResponse(id, from) ⇒ {
                  logger info "Signing in to message with id: [%s] from [%s]".format(id, from)
                  val buff = ChannelBuffers.copiedBuffer(<handshake>{ config.asHexSecret(id) }</handshake>.toString, Utf8)
                  e.getChannel.write(buff)
                }
                case source ⇒ {
                  logger debug "Reading stanza: %s".format (source)
                  ReadStanza(source) foreach {
                    case <handshake/> ⇒ {
                      logger info "Established connection to [%s:%d]".format(config.host, config.port)
                      actor ! Scapulet.Connected
                    }
                    case x: Node ⇒ actor ! IncomingStanza(x)
                    case _       ⇒
                  }
                }
              }
            }
          }
          case _ ⇒ ctx.sendUpstream(e)
        }
      }
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      logger.error(e.getCause, "There was a problem in the XMPP Channel Handler")
      e.getChannel.close
    }

    private def logError(th: Throwable) = {
      logger.error(th, "Unexpected exception in component connection [{}]", config.address)
    }
  }

  trait Handler {
    def predicate: Predicate
    def name: String
  }
  case class RegisterHandler(predicate: Predicate, handler: Props, name: String) extends Handler
  case class ActorHandler(predicate: Predicate, handler: ActorRef) extends Handler {
    def name = handler.path.toStringWithAddress(handler.path.address)
  }
}

private[scapulet] class ComponentConnection(overrideConfig: Option[ComponentConfig] = None, callback: Option[ActorRef] = None) extends ScapuletConnectionActor {

  def this() = this(None)

  import ComponentConnection._
  val config = overrideConfig getOrElse context.system.scapulet.settings.component(self.path.name)

  private var connection: NettyClient = null
  private var authenticated = false
  private var handlers = Vector.empty[Handler]

  override def preStart() {
    self ! Scapulet.Connect
  }

  override def postStop() {
    if (connection != null) connection.close()
    logger info "XMPP component [%s] disconnected from host [%s:%d].".format(
      config.connection.address,
      config.connection.host,
      config.connection.port)
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    logger error (reason, "Reconnecting after connection problem")
    super.preRestart(reason, message)
  }

  protected def receive = {
    case xml: NodeSeq ⇒ connection.write(xml)
    case Scapulet.Connect ⇒ {
      logger info "Starting netty client connection"
      implicit val system = context.system
      if (connection == null)
        connection = new NettyClient(config.connection, Channels.pipeline(new ComponentConnectionHandler(config.connection)))
      connection.connect()
    }
    case Scapulet.Connected ⇒ {
      logger info "XMPP component session %s established".format(self.path.name)
      authenticated = true
      callback foreach { _ ! Scapulet.Connected }
    }
    case Scapulet.Reconnect ⇒ {
      callback foreach { _ ! Scapulet.Reconnect }
      connection.reconnect()
    }
    case Scapulet.Disconnect ⇒ {
      callback foreach { _ ! Scapulet.Disconnect }
      connection.disconnect()
    }
    case Scapulet.Disconnected ⇒ context stop self
    case IncomingStanza(stanza) if authenticated ⇒ {
      (handlers filter (_.predicate(stanza)) map (h ⇒ context.actorFor(h.name))).distinct foreach { _ ! stanza }
    }
    case _: IncomingStanza ⇒ throw new IllegalStateException("Received a stanza before being authenticated")
    case h @ RegisterHandler(_, props, name) ⇒ {
      val created = context.actorOf(props, name)
      context watch created
      handlers :+= h
      sender ! created
    }
    case h: ActorHandler ⇒ {
      handlers :+= h
      context watch h.handler
    }
    case Terminated(actor) ⇒ {
      handlers = handlers filterNot {
        case ActorHandler(_, h) ⇒ h == actor
        case x                  ⇒ context.actorFor(x.name) == actor
      }
    }
  }
}

