package io.backchat.scapulet

import stanza.{ Identity, Feature }
import util.control.Exception._
import _root_.org.jboss.netty.channel._
import akka.actor._
import _root_.org.jboss.netty.buffer.{ ChannelBuffers, ChannelBuffer }
import xml._
import jivesoftware.openfire.nio.XMLLightweightParser
import io.backchat.scapulet.Stanza.Predicate
import akka.pattern._
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import java.util.concurrent.TimeoutException
import com.typesafe.config.Config
import akka.dispatch.{ UnboundedPriorityMailbox, PriorityGenerator, Await, Future }

object XmppComponent {

  def apply(
    id: String,
    overrideConfig: Option[ComponentConfig] = None)(implicit system: ActorSystem): io.backchat.scapulet.XmppComponent = {
    require(overrideConfig.isDefined || system.settings.config.hasPath("scapulet.components." + id), "You must define the component in the configuration")
    val retrieved = system.actorFor(system.scapulet.components.path / id)
    val comp = if (retrieved.isTerminated) {
      system.scapulet.componentConnection(id, overrideConfig)
    } else retrieved
    new XmppComponent(id, comp)
  }

  private[scapulet] class XmppComponent(val id: String, component: ActorRef)(implicit val system: ActorSystem) extends io.backchat.scapulet.XmppComponent {

    import StanzaHandler.Messages._

    private val duration: Duration = 2 seconds

    private implicit val timeout = Timeout(duration)

    def features: Seq[Feature] = {
      ask[Seq[Feature]](features)
    }

    def identities: Seq[Identity] = {
      ask[Seq[Identity]](Identities)
    }

    def register(handler: StanzaHandler) = {
      component ! Register(handler)
    }

    def unregister(handler: StanzaHandler) = {
      component ! Unregister(handler)
    }

    def stop() {
      system stop component
    }

    def send(node: Node) = component ! node

    def ask[TResult](msg: Any)(implicit mf: Manifest[TResult]) = {
      Await.result((component ? msg).mapTo[TResult], duration)
    }
  }
}

/**
 * A XMPP Component API convenience interface
 * You can get an instance of this component by asking the actor system extension for a component by id
 * {{
 *  system.scapulet.component("weather")
 * }}
 *
 * Once a component has been started and set up you can also get a component by requesting it by actor path
 * {{
 *   system.actorFor("/user/xmpp/components/weather")
 * }}
 */
trait XmppComponent {

  /**
   * The ID this component is known by and by which it can be looked up. This id is the id you put in the
   * config to configure the component.
   * You can get a component by requesting it by id from the actor system extension or by actor path
   *
   * @return a string representing the component id
   */
  def id: String

  /**
   * gets the list of features this components supports, this is used in the built-in service discovery
   *
   * @return a sequence of [[io.backchat.scapulet.stanza.Feature]]
   */
  def features: Seq[Feature]

  /**
   * gets the list of identities for this component, this is used in the built-in service discovery
   *
   * @return a sequence of [[io.backchat.scapulet.stanza.Feature]]
   */
  def identities: Seq[Identity]

  /**
   * Register a new stanza handler with this component.
   * As soon as the handler is registered it will participate in the incoming stanza handling.
   *
   * @param handler The new [[io.backchat.scapulet.StanzaHandler]]
   */
  def register(handler: StanzaHandler): Unit

  /**
   * Unregister a stanza handler.
   * @param handler
   */
  def unregister(handler: StanzaHandler): Unit

  /**
   * Stop this component
   */
  def stop(): Unit

  /**
   * Sends a xml snippet to this components connection
   *
   * @param node A [[scala.xml.Node]]
   */
  def send(node: Node): Unit

  /**
   * @see this#send
   */
  def !(node: Node): Unit = send(node)

  /**
   * Sends the connection a message and returns a result.
   *
   * @param msg The message to get a reply for
   * @param mf  implicit parameter for the manfest of the result type
   * @tparam TResult the result type of the ask operation
   * @throws java.util.concurrent.TimeoutException
   * @return an instance of TResult
   */
  @throws(classOf[TimeoutException])
  def ask[TResult](msg: Any)(implicit mf: Manifest[TResult]): TResult

  /**
   * @see this#ask
   */
  @throws(classOf[TimeoutException])
  def ?[TResult](msg: Any)(implicit mf: Manifest[TResult]): TResult = ask(msg)
}

object ComponentConnection {

  private[scapulet] object OpenStream {
    val openStreamFormatString = """<stream:stream xmlns="%s" xmlns:stream="%s" to="%s" >"""
    private val streamRegex = """(<\?[^?]>)?<stream:stream[^>]*>""".r
    def apply(jid: String, nsd: String = ns.component.Accept) = openStreamFormatString.format(nsd, ns.Stream, jid)
    def unapply(msg: String) = streamRegex.findFirstMatchIn(msg) match {
      case Some(start) ⇒ {
        val x = XML.loadString(start + "</stream:stream>")
        Some(x \ "@from" text)
      }
      case _ ⇒ None
    }
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

  private[scapulet] case class Handler(predicate: Predicate, handlesQuery: Any ⇒ Boolean, handler: ActorRef)

  class ComponentConnectionHandle(compontentId: String, config: ConnectionConfig) extends ScapuletConnectionActor {

    private var connection: NettyClient = null
    private var authenticated = false

    override def postStop() {
      if (connection != null) connection.close()
      logger info "XMPP component [%s] disconnected from host [%s:%d].".format(
        config.address,
        config.host,
        config.port)
    }

    override def preRestart(reason: Throwable, message: Option[Any]) {
      logger error (reason, "Reconnecting after connection problem")
      super.preRestart(reason, message)
    }

    protected def receive = {
      case xml: NodeSeq if authenticated ⇒ connection.write(xml)
      case xml: NodeSeq ⇒ {
        logger warning ("Can't send xml before a connection has been established, dropping:\n{}", xml.toString)
      }
      case Scapulet.Connect ⇒ {
        logger info "Starting netty client connection"
        implicit val system = context.system
        if (connection == null)
          connection = new NettyClient(config, Channels.pipeline(new ComponentConnectionHandler(config)))
        connection.connect()
      }
      case Scapulet.Connected ⇒ {
        logger info "[%s] XMPP component session established".format(context.parent.path.name)
        authenticated = true
        context.parent ! Scapulet.Connected
      }
      case Scapulet.Reconnect ⇒ {
        connection.reconnect()
      }
      case Scapulet.Disconnect ⇒ {
        context.parent ! Scapulet.Disconnect
        connection.disconnect()
      }
      case Scapulet.Disconnected                ⇒ context.parent ! Scapulet.Disconnected
      case evt: IncomingStanza if authenticated ⇒ context.parent ! evt
      case evt: IncomingStanza ⇒ {
        logger warning ("Received a stanza while not authenticated, that's wrong. The stanza:\n{}", evt)
      }

    }
  }

  val priorityGenerator = PriorityGenerator {
    case _: StanzaHandler.Messages.ScapuletHandlerRequest ⇒ 10
    case PoisonPill ⇒ 1000
    case _: IncomingStanza ⇒ 100
    case _ ⇒ 50
  }

  class Mailbox(config: Config) extends UnboundedPriorityMailbox(priorityGenerator)
}

private[scapulet] class ComponentConnection(
    overrideConfig: Option[ComponentConfig] = None) extends ScapuletConnectionActor {

  import ComponentConnection._
  import StanzaHandler.Messages._

  implicit val executor = context.system.dispatcher
  val config = overrideConfig getOrElse context.system.scapulet.settings.component(self.path.name)

  protected val callback = config.connection.connectionCallback

  implicit val timeout = Timeout(config.requestTimeout)

  private var _handlers = Set.empty[Handler]

  protected def connection = context.actorFor("connection")

  protected def receive = handlerMessages orElse connectionMessages orElse lifeCycleMessages

  protected def lifeCycleMessages: Receive = {
    case Terminated(actor) ⇒ {
      _handlers = _handlers filterNot (_ == actor)
    }
  }

  protected def handlerMessages: Receive = {
    case ComponentInfos                  ⇒ nodeSeqQuery(_handlers, ComponentInfos)
    case Request(ComponentInfos, seenBy) ⇒ nodeSeqQuery(_handlers, ComponentInfos, seenBy)
    case Items                           ⇒ nodeSeqQuery(_handlers, Items)
    case Request(Items, seenBy)          ⇒ nodeSeqQuery(_handlers, Items, seenBy)
    case Infos                           ⇒ nodeSeqQuery(_handlers, Infos)
    case Request(Infos, seenBy)          ⇒ nodeSeqQuery(_handlers, Infos, seenBy)
    case ServerInfos                     ⇒ nodeSeqQuery(_handlers, ServerInfos)
    case Request(ServerInfos, seenBy)    ⇒ nodeSeqQuery(_handlers, ServerInfos, seenBy)
    case m @ (_: ScapuletHandlerRequest) ⇒ query(_handlers, m)
    case Request(req, seenBy)            ⇒ query(_handlers, req, seenBy)
    case h: Handler                      ⇒ addHandler(h)
    case Register(handler)               ⇒ registerHandler(handler)
    case Unregister(handler)             ⇒ context stop context.actorFor(handler.handlerId)
  }

  protected def connectionMessages: Receive = {
    case xml: NodeSeq ⇒ connection ! xml
    case Scapulet.Connect ⇒ {
      val c = if (connection.isTerminated) {
        val conn = context.actorOf(Props(new ComponentConnectionHandle(self.path.name, config.connection)), "connection")
        context.watch(conn)
        conn
      } else connection

      c ! Scapulet.Connect
    }
    case Scapulet.Connected ⇒ {
      callback foreach { _ ! Scapulet.Connected }
    }
    case Scapulet.Disconnect ⇒ {
      callback foreach { _ ! Scapulet.Disconnect }
    }
    case Scapulet.Disconnected  ⇒ context stop self
    case IncomingStanza(stanza) ⇒ macthingHandlers(stanza) foreach { _ ! stanza }
  }

  private def macthingHandlers(stanza: Node) = _handlers filter (_.predicate(stanza)) map (_.handler)

  private def nodeSeqQuery(handlers: Set[Handler], msg: ScapuletHandlerRequest, seenBy: Seq[ActorRef] = Seq.empty) {
    doQuery[NodeSeq](handlers, msg, seenBy) map (_ reduce (_ ++ _)) pipeTo sender
  }
  private def query[Response](handlers: Set[Handler], msg: ScapuletHandlerRequest, seenBy: Seq[ActorRef] = Seq.empty)(implicit mf: Manifest[Response]) {
    doQuery(handlers, msg, seenBy) pipeTo sender
  }
  private def doQuery[Response](handlers: Set[Handler], msg: ScapuletHandlerRequest, seenBy: Seq[ActorRef] = Seq.empty)(implicit mf: Manifest[Response]): Future[Seq[Response]] = {
    val futures = handlers map (_.handler) filterNot seenBy.contains map { h ⇒ (h ? Request(msg, seenBy)).mapTo[Seq[Response]] }
    Future.reduce(futures)((_: Seq[Response]) ++ _)
  }

  private def addHandler(handler: Handler) = {
    context.watch(handler.handler)
    _handlers += handler
  }

  private def registerHandler(scapuletHandler: StanzaHandler) = {
    val predicate = Stanza.matching(
      scapuletHandler.handlerId + "-predicate",
      { case x ⇒ scapuletHandler.handleStanza.isDefinedAt(x) })
    val props = Props(new StanzaHandler.ScapuletHandlerHost(scapuletHandler)).withDispatcher("scapulet.component-connection-dispatcher")
    val actor = context.actorOf(props, scapuletHandler.handlerId)
    scapuletHandler.actor = actor
    val recv = scapuletHandler.handleMeta(null)
    val handlesQuery = (m: Any) ⇒ recv.isDefinedAt(m)
    val handler = Handler(predicate, handlesQuery, actor)
    addHandler(handler)
  }
}

