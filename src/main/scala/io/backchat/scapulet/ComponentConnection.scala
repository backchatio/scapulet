package io.backchat.scapulet

import util.control.Exception._
import org.jboss.netty.channel._
import akka.actor._
import org.jboss.netty.buffer.{ ChannelBuffers, ChannelBuffer }
import xml._

object ComponentConnection {

  object OpenStream {
    val openStreamFormatString = """<stream:stream xmlns="%s" xmlns:stream="%s" to="%s" >"""
    def apply(jid: String, nsd: String = ns.component.Accept) = openStreamFormatString.format(nsd, ns.Stream, jid)
  }

  object StreamResponse {
    private val streamRegex = """(<\?[^?]>)?<stream:stream[^>]*>""".r

    def unapply(msg: String) = streamRegex.findFirstMatchIn(msg) match {
      case Some(start) ⇒
        val x = XML.loadString(start + "</stream:stream>")
        Some(((x \ "@id").text, (x \ "@from").text))
      case _ ⇒ None
    }
  }

  object AuthenticationFailureResponse {
    private val regex = "(<stream:error>.*</stream:error>)(.*)".r

    def unapply(msg: String) = regex.findFirstMatchIn(msg) match {
      case Some(m) ⇒ {
        val x = XML.loadString(m.group(1))
        Some((x \ "text").text)
      }
      case _ ⇒ None
    }
  }

  class ComponentConnectionHandler(config: ConnectionConfig)(implicit protected val system: ActorSystem, actor: ActorRef) extends SimpleChannelUpstreamHandler with Logging {
    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      logger info ("Connected to server, authenticating...")
      val buff = ChannelBuffers.copiedBuffer(OpenStream(config.address), Utf8)
      e.getChannel.write(buff)
    }

    override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      actor ! Scapulet.Disconnected
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
            b.toString(Utf8) match {
              case AuthenticationFailureResponse(error) ⇒ {
                logger error error
                throw new UnauthorizedException(error)
              }
              case StreamResponse(id, from) ⇒ {
                logger info "Signing in to message with id: [%s] from [%s]".format(id, from)
                val buff = ChannelBuffers.copiedBuffer(<handshake>{ config.asHexSecret(id) }</handshake>.toString, Utf8)
                e.getChannel.write(buff)
              }
              case source ⇒ {
                ReadXml(source) foreach {
                  case <handshake/> ⇒ {
                    logger info "Established connection to [%s:%d]".format(config.host, config.port)
                    actor ! Scapulet.Connected
                  }
                  case x: Node ⇒ system.scapulet.eventStream publish x
                  case _       ⇒
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
}
class ComponentConnection extends ScapuletConnectionActor {

  import ComponentConnection.ComponentConnectionHandler
  val config = context.system.scapulet.components(self.path.name)

  private var connection: NettyClient = null

  override def preStart() {
    self ! Scapulet.Connect
  }

  override def postStop() {
    if (connection != null) connection.disconnect()
    logger info "XMPP component [%s] disconnected from host [%s:%d].".format(config.address, config.host, config.port)
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    logger error (reason, "Reconnecting after connection problem")
    super.preRestart(reason, message)
  }

  protected def receive = {
    case xml: NodeSeq       ⇒ connection.write(xml)
    case Scapulet.Reconnect ⇒ connection.reconnect()
    case Scapulet.Connect ⇒ {
      implicit val system = context.system
      if (connection == null) connection = new NettyClient(config, Channels.pipeline(new ComponentConnectionHandler(config)))
      connection.connect()
    }
  }
}

//class ComponentConnection extends Actor {
//
//  val state = IO.IterateeRef.Map.async[IO.Handle]()(context.dispatcher)
//
//  override def preStart {
//    IOManager(context.system) listen new InetSocketAddress(port)
//  }
//
//  def receive = {
//    case bytes: ByteString =>
//    case IO.NewClient(server) ⇒
//      val socket = server.accept()
//      state(socket) flatMap (_ => processConnect)
//
//    case IO.Read(socket, bytes) ⇒
//      state(socket)(IO.Chunk(bytes))
//
//    case IO.Closed(socket, cause) ⇒
//      state(socket)(IO.EOF(None))
//      state -= socket
//
//  }
//
//  protected def processConnect(socket: IO.SocketHandle): IO.Iteratee[Elem] = {
//    for { ele <- loadXml } yield ele
//  }
//
//  protected def loadXml = {
//    for {
//      chunk <- IO.takeAll
//      ele <- readChunk(chunk.utf8String)
//    } yield ele
//  }
//
//  private def readChunk(source: String): Seq[Elem] =
//    (catching(classOf[SAXParseException]) withApply wrap(source)_) {
//      List(XML.loadString(source))
//    }
//  private def wrap(source: String)(th: Throwable) = XML.loadString("<wrapper>%s</wrapper>".format(source)).child.toList
//}
