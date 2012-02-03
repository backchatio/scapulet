package io.backchat.scapulet

import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.group.{ChannelGroupFuture, ChannelGroupFutureListener, DefaultChannelGroup}
import org.jboss.netty.channel._
import io.backchat.scapulet.Scapulet._
import org.jboss.netty.handler.codec.string.{StringDecoder}
import org.jboss.netty.util.{Timeout, TimerTask, HashedWheelTimer, Timer}
import xml._
import java.net.{Socket, InetSocketAddress}
import java.io._
import org.jboss.netty.buffer.ChannelBuffers
import StringUtil.{Utf8, hash}
import java.util.concurrent.{TimeUnit, Executors}
import io.backchat.scapulet.Exceptions.UnauthorizedException
import akka.actor.{ActorSystem, ActorRef}

object ComponentConnection {


  private[scapulet] object XMPPStrings {
    val NS_ACCEPT = "jabber:component:accept"
    val NS_CONNECT = "jabber:component:connect"
    val NS_STREAM = "http://etherx.jabber.org/streams"
    val OPEN_STREAM_FMT = """<stream:stream xmlns="%s" xmlns:stream="%s" to="%s" >"""

    def makeStreamMessage(jid: String, ns: String = NS_ACCEPT) =
      OPEN_STREAM_FMT.format(ns, NS_STREAM, jid)
  }

  import XMPPStrings._


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



  class FaultTolerantComponentConnection(connectionConfig: ConnectionConfig)(implicit protected val system: ActorSystem) extends ScapuletConnection with Logging {

    import connectionConfig._

    def address = connectionConfig.address

    private var _xmlProcessorOption: Option[ActorRef] = None

    def xmlProcessor = _xmlProcessorOption

    def xmlProcessor_=(processor: ActorRef) = _xmlProcessorOption = Option(processor)


    val reconnectionTimer = new HashedWheelTimer

    def hexCredentials(id: String) = {
      StringUtil.hash("%s%s".format(id, password))
    }

    private var isConnected = false
    private val channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool, Executors.newCachedThreadPool)
    private val bootstrap = new ClientBootstrap(channelFactory)
    val serverAddress = new InetSocketAddress(host, port)

    private var _connection: ChannelFuture = _
    private val openHandlers = new DefaultChannelGroup(classOf[FaultTolerantComponentConnection].getName)


    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline = Channels.pipeline(
        new StringDecoder(Utf8),
        new AuthenticateHandler(bootstrap, reconnectionTimer, connectionConfig, _xmlProcessorOption, conn))
    })
    bootstrap.setOption("connectTimeoutMillis", connectionTimeout.toMillis)
    bootstrap.setOption("tcpNoDelay", true)
    bootstrap.setOption("keepAlive", true)

    val conn = this

    def sayGoodbye(nodes: NodeSeq)(callback: => Unit) = {
      val txt = nodes.map(Utility.trimProper _).toString
      logger debug "Saying goodbye to:\n%s".format(txt)
      val buff = ChannelBuffers.copiedBuffer(txt, Utf8)
      val writeFuture = openHandlers.write(buff)
      writeFuture.addListener(new ChannelGroupFutureListener {
        def operationComplete(fut: ChannelGroupFuture) = {
          if (fut.isDone) {
            disconnect
            callback
          }
        }
      })
      writeFuture.awaitUninterruptibly(5, TimeUnit.SECONDS)
    }

    def write(nodes: Seq[Node]) {
      val txt = nodes.map(Utility.trimProper _).toString
      val buff = ChannelBuffers.copiedBuffer(txt, Utf8)
      val writeFuture = openHandlers.write(buff)
      writeFuture.addListener(new ChannelGroupFutureListener {
        def operationComplete(fut: ChannelGroupFuture) = {
          if (!fut.isCompleteSuccess) {
            logger error "Failed to send: %s".format(nodes)
          }
        }
      })
      //writeFuture.awaitUninterruptibly(3, TimeUnit.SECONDS)
    }

    private[scapulet] def connection_=(newConnection: ChannelFuture) = _connection = newConnection

    private[scapulet] def connection = _connection

    def connect: Unit = synchronized {
      if (!isConnected) {
        _connection = bootstrap connect serverAddress
        logger info "Connecting to XMPP server at [%s:%s]".format(host, port)
        _connection.awaitUninterruptibly
        logger debug "The connection is done: %s".format(_connection.isDone)
        val ch = _connection.getChannel
        openHandlers.add(ch)
        if (_connection.isCancelled) {
          logger info "Connection cancelled by user, exiting."
          disconnect
        }
        if (!_connection.isSuccess) {
          logger.error(_connection.getCause, "XMPP connection has failed, trying to reconnect in %s seconds.".format(reconnectDelay.toSeconds))
        }
        isConnected = true
      }
    }

    private[ComponentConnection] def notifyCallback(message: ComponentConnectionMessage) =
      connectionCallback foreach {
        cb => if (!cb.isTerminated) cb ! message
      }

    def connected_? = isConnected

    def disconnect = synchronized {
      if (isConnected) {
        isConnected = false
        openHandlers.disconnect
        openHandlers.close.awaitUninterruptibly
        bootstrap.releaseExternalResources
        logger info "XMPP component [%s] disconnected from host [%s:%d].".format(address, host, port)
        _connection = null
      }
    }

  }

  class StanzaDecoder extends SimpleChannelHandler {

    override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
      val nodes = e.getMessage
      val txt = nodes match {
        case msg: Node => msg.map(Utility.trimProper _).toString
        case msg: String => msg
      }
      val buff = ChannelBuffers.copiedBuffer(txt, Utf8)
      Channels.write(ctx, e.getFuture, buff)
    }
  }

  @ChannelHandler.Sharable
  class AuthenticateHandler(
                             bootstrap: ClientBootstrap,
                             timer: Timer,
                             config: ConnectionConfig,
                             xmlProcessor: Option[ActorRef],
                             connection: FaultTolerantComponentConnection)(implicit protected val system: ActorSystem) extends SimpleChannelHandler with Logging {

    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      if (e.getState == ChannelState.CONNECTED) {
        logger info ("Connected to server, authenticating...")
        val buff = ChannelBuffers.copiedBuffer(makeStreamMessage(connection.address), Utf8)
        e.getChannel.write(buff)

      }
    }

    override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      connection.notifyCallback(Disconnected)
    }

    override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      val cfg = config
      if (connection.connected_?) {
        logger warning "XMPP connection to [%s:%s] has failed, trying to reconnect in %d seconds.".format(
          config.host, config.port, config.reconnectDelay.toSeconds)
        timer.newTimeout(new TimerTask {
          override def run(timeout: Timeout) = {
            logger info "Reconnecting"
            connection.notifyCallback(Reconnecting)
            connection.connection = bootstrap connect connection.serverAddress
            connection.connection.awaitUninterruptibly
            logger.error(connection.connection.getCause, "Reconnection to [%s:%d] has failed!".format(config.host, config.port))
          }
        }, config.reconnectDelay.length, config.reconnectDelay.unit)
      }
    }

    private def loadXml(source: String) = {
      try {
        List(XML.loadString(source))
      } catch {
        case e: SAXParseException => {
          val doc = XML.loadString("<wrapper>%s</wrapper>".format(source))
          doc.child.toList
        }
      }

    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      try {
        val ch = e.getChannel

        val msg = e.getMessage.asInstanceOf[String]
        logger debug "Message received: %s".format(msg)
        msg match {
          case AuthenticationFailureResponse(error) => {
            logger error error
            throw new UnauthorizedException(error)
          }
          case StreamResponse(id, from) => {
            logger info "Signing in to message with id: [%s] from [%s]".format(id, from)
            ch.write(ChannelBuffers.copiedBuffer(<handshake>
              {connection.hexCredentials(id)}
            </handshake>.toString, Utf8))
          }
          case s => {
            loadXml(s) foreach {
              x => x match {
                case <handshake/> => {
                  logger info "Established connection to [%s:%d]".format(config.host, config.port)
                  connection.notifyCallback(Connected)
                }
                case x: Node => xmlProcessor.foreach(a => a ! Utility.trim(x))
                case _ =>
              }
            }
          }
        }
      } catch {
        case e: Exception => {
          logger.error(e, "Unexpected exception in component connection [%s]" format connection.address)
        }
      }

    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      logger.error(e.getCause, "There was a problem in the XMPP Channel Handler")
      e.getChannel.close
    }
  }

}
