package io.backchat.scapulet

import stanza.{ Identity, Feature }
import util.control.Exception._
import _root_.org.jboss.netty.channel._
import akka.actor._
import _root_.org.jboss.netty.buffer.{ ChannelBuffers, ChannelBuffer }
import xml._
import jivesoftware.openfire.nio.XMLLightweightParser
import io.backchat.scapulet.Stanza.Predicate
import akka.pattern.ask
import akka.dispatch.{ Await, Future }
import akka.util.duration._

object XmppComponent {

  def apply(id: String, overrideConfig: Option[ComponentConfig] = None, callback: Option[ActorRef] = None)(implicit system: ActorSystem): io.backchat.scapulet.XmppComponent =
    new XmppComponent(id, overrideConfig, callback)

  private[scapulet] class XmppComponent(val id: String, overrideConfig: Option[ComponentConfig] = None, callback: Option[ActorRef] = None)(implicit val system: ActorSystem) extends io.backchat.scapulet.XmppComponent {

    import ScapuletHandler.Messages._
    private implicit val timeout = system.settings.ActorTimeout
    protected val connection = system.scapulet.componentConnection(this, overrideConfig, callback)

    def features: Seq[Feature] = {
      Await.result((connection ? Features).mapTo[Seq[Feature]], 2 seconds)
    }

    def identities: Seq[Identity] = {
      Await.result((connection ? Identities).mapTo[Seq[Identity]], 2 seconds)
    }

    def register(handler: ScapuletHandler) = {
      connection ! Register(handler)
    }

    def unregister(handler: ScapuletHandler) = {
      connection ! Unregister(handler)
    }

    def stop() {
      system stop connection
    }

    def write(node: Node) = connection ! node
  }
}

trait XmppComponent {

  def id: String

  def features: Seq[Feature]

  def identities: Seq[Identity]

  def register(handler: ScapuletHandler)

  def unregister(handler: ScapuletHandler)

  def stop()

  def write(node: Node)
}

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

  private[scapulet] case class Handler(predicate: Predicate, handlesQuery: Any => Boolean, handler: ActorRef)
  case object Component

  class ComponentConnectionHandle()
}

private[scapulet] class ComponentConnection(component: XmppComponent, overrideConfig: Option[ComponentConfig] = None, callback: Option[ActorRef] = None) extends ScapuletConnectionActor {

  import ComponentConnection._
  import ScapuletHandler.Messages._

  implicit val timeout = context.system.settings.ActorTimeout
  implicit val executor = context.system.dispatcher
  val config = overrideConfig getOrElse context.system.scapulet.settings.component(self.path.name)

  private var connection: NettyClient = null
  private var authenticated = false
  private var _handlers = Set.empty[Handler]

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
      // TODO: Start handlers from configuration
    }
    case Scapulet.Connected ⇒ {
      logger info "XMPP component session %s established".format(component.id)
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
      (_handlers filter (_.predicate(stanza)) map (_.handler)) foreach { _ ! stanza }
    }
    case _: IncomingStanza ⇒ throw new IllegalStateException("Received a stanza before being authenticated")
    case Terminated(actor) ⇒ {
      _handlers = _handlers filterNot (_ == actor)
    }
    case m @ (_: ScapuletHandlerRequest) ⇒ query(_handlers, m)
    case h: Handler ⇒ addHandler(h)
    case Register(handler) ⇒ registerHandler(handler)
    case Component ⇒ sender ! component
    case Unregister(handler) ⇒ {
      context stop context.actorFor(handler.handlerId)
    }
  }
  
  private def query[Response](handlers: Seq[Handler], msg: ScapuletHandlerMessage) {
    val replyTo = sender
    val futures = handlers map (h ⇒ (h.handler ? msg).mapTo[Seq[Feature]])
    Future.reduce(futures)((_: Seq[Response]) ++ _) onSuccess {
      case results ⇒ replyTo ! results
    }
  }
  
  private def addHandler(handler: Handler) = {
    context.watch(h.handler)
    _handlers += h    
  }
  
  private def registerHandler(scapuletHandler: ScapuletHandler) = {
    val predicate = Stanza.matching(
      scapuletHandler.handlerId + "-predicate", 
      { case x ⇒ scapuletHandler.handleStanza.isDefinedAt(x) })
    val props = Props(new ScapuletHandler.ScapuletHandlerHost(handler))
    val actor = context.actorOf(props, scapuletHandler.handlerId)
    scapuletHandler.actor = actor
    val recv = scapuletHandler.handleMeta(null)
    val handlesQuery = (m: Any) => recv.isDefinedAt(m)
    val handler = Handler(predicate, handlesQuery, actor)
    addHandler(handler)
  }
  
  private def nonServiceDiscoveryHandlers = _handlers filter {_.handler.path.name != "service-discovery" }
  
}

