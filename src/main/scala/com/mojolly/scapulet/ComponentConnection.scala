package com.mojolly.scapulet

import java.net.InetSocketAddress
import se.scalablesolutions.akka.util.Logging
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import group.DefaultChannelGroup
import org.jboss.netty.handler.codec.string.{StringEncoder, StringDecoder}
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.config.OneForOneStrategy
import com.mojolly.scapulet.Exceptions.UnauthorizedException
import org.jboss.netty.handler.timeout.ReadTimeoutHandler
import org.jboss.netty.util.{Timeout, TimerTask, HashedWheelTimer, Timer}
import se.scalablesolutions.akka.actor.{ActorRegistry, Exit, Actor, ActorRef}
import com.mojolly.scapulet.Scapulet.{RegisterSendCallback, ConnectionConfig}
import xml.parsing.{NoBindingFactoryAdapter, FactoryAdapter}
import org.xml.sax.InputSource
import javax.xml.parsers.{SAXParserFactory, SAXParser}
import xml._

/**
 * Created by IntelliJ IDEA.
 * User: ivan
 * Date: Aug 16, 2010
 * Time: 10:04:01 PM
 * 
 */

object ComponentConnection {

  object Messages {
    sealed trait ComponentConnectionMessage
    case object Connect extends ComponentConnectionMessage
    case object Connected extends ComponentConnectionMessage
    case object Reconnecting extends ComponentConnectionMessage
    case object Disconnected extends ComponentConnectionMessage
    case class Failure(cause: Throwable) extends ComponentConnectionMessage
    case class ConnectionShutdown(cause: Throwable) extends ComponentConnectionMessage
    case class Send(xml: Seq[Node]) extends ComponentConnectionMessage


  }
  import Messages._

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
        Some((x \ "@id").text, (x \ "@from").text)        
      case _ => None
    }
  }

  object AuthenticationFailureResponse {
    private val regex = "(<stream:error>.*</stream:error>)(.*)".r
    def unapply(msg: String) = regex.findFirstMatchIn(msg) match {
      case Some(m) => {
        val x = XML.loadString(m.group(1))
        Some((x \ "text").text )
      }
      case _ => None
    }
  }

//  class PermissiveXmlParserFactoryAdapter extends NoBindingFactoryAdapter with Logging {
//
//    override def parser = try {
//        val f = SAXParserFactory.newInstance
//        f.setNamespaceAware(false)
//        f.newSAXParser
//      } catch {
//        case e => {
//          log.error(e, "Unable to create SAX parser")
//          throw e
//        }
//      }
//
//  }
  

  class FaultTolerantComponentConnection(connectionConfig: ConnectionConfig) extends Logging {
    import connectionConfig._

    def address = connectionConfig.address

    private var _xmlProcessorOption: Option[ActorRef] = None

    def xmlProcessor = _xmlProcessorOption
    def xmlProcessor_=(processor: ActorRef) = _xmlProcessorOption = Option(processor)


    val reconnectionTimer = new HashedWheelTimer

    def hexCredentials(id: String) = StringUtil.hash(id + password)

    private var isConnected = false
    private val channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool, Executors.newCachedThreadPool)
    private val bootstrap = new ClientBootstrap(channelFactory)
    val serverAddress = new InetSocketAddress(host, port)

    private var _connection: ChannelFuture = _
    private val openHandlers = new DefaultChannelGroup(classOf[FaultTolerantComponentConnection].getName)


    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline = Channels.pipeline(
        new StringDecoder(StringUtil.Utf8),
        new AuthenticateHandler(bootstrap, reconnectionTimer, connectionConfig, _xmlProcessorOption, conn),
        new StringEncoder(StringUtil.Utf8))
    })
    bootstrap.setOption("connectTimeoutMillis", connectionTimeout.toMillis)
    bootstrap.setOption("tcpNoDelay", true)
    bootstrap.setOption("keepAlive", true)

//    protected def receive = {
//      case Connect => connect
//      case ConnectionShutdown(cause) => {
//        log.error(cause, "There has been an error in the xmpp connection [%s] to [%s:%d]".format(address, host, port))
//        self ! Exit(self, cause)
//      }
//    }
    val conn = this

    private[scapulet] def connection_=(newConnection: ChannelFuture) = _connection = newConnection
    private[scapulet] def connection = _connection
    def connect: Unit = synchronized {
      if(!isConnected) {
        _connection = bootstrap connect serverAddress
        log info "Connecting to XMPP server at [%s:%s]".format(host, port)
        _connection.awaitUninterruptibly
        log debug "The connection is done: %s".format(_connection.isDone)
        val ch = _connection.getChannel
        openHandlers.add(ch)
        if(_connection.isCancelled) {
          log info "Connection cancelled by user, exiting."
          disconnect
        }
        if(!_connection.isSuccess) {
          log.error(_connection.getCause, "XMPP connection has failed, trying to reconnect in %s seconds.".format(reconnectDelay.toSeconds))
//            reconnectionTimer.newTimeout(new TimerTask {
//            override def run(timeout: Timeout) = {
//              notifyCallback(Reconnecting)
//              connect
//            }
//          }, reconnectDelay.length, reconnectDelay.unit)
        }
        isConnected = true
      }
    }

    private[ComponentConnection] def notifyCallback(message: ComponentConnectionMessage) =
      connectionCallback foreach { cb => if(cb.isRunning) cb ! message}

    def connected_? = isConnected
    def disconnect = synchronized {
      if(isConnected) {
        isConnected = false
        openHandlers.disconnect
        openHandlers.close.awaitUninterruptibly
        bootstrap.releaseExternalResources
        ActorRegistry.shutdownAll
        log info "XMPP component [%s] disconnected from host [%s:%d].".format(address, host, port)
        _connection = null
      }
    }

//    override def shutdown = {
//      reconnectionTimer.cancel
//      self.shutdownLinkedActors
//      disconnect
//    }
//
//    override def preRestart(cause:Throwable) = {
//      disconnect
//    }
//
//    override def postRestart(cause: Throwable) = {
//      notifyCallback(Reconnecting)
//      self ! Connect
//    }
  }

  @ChannelHandler.Sharable
  class AuthenticateHandler(
      bootstrap: ClientBootstrap,
      timer: Timer,
      config: ConnectionConfig,
      xmlProcessor: Option[ActorRef],
      connection: FaultTolerantComponentConnection) extends SimpleChannelHandler with Logging {

    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      if(e.getState == ChannelState.CONNECTED) {
        log info ("Connected to server, authenticating...")
        val buff = makeStreamMessage(connection.address)
        e.getChannel.write(buff)

      }
    }

    override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      connection.notifyCallback(Disconnected)
    }

    override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      val cfg = config
      if(connection.connected_?) {
        log warning "XMPP connection to [%s:%s] has failed, trying to reconnect in %d seconds.".format(
          config.host, config.port, config.reconnectDelay.toSeconds)
        timer.newTimeout(new TimerTask {
          override def run(timeout: Timeout) = {
            log info "Reconnecting"
            connection.notifyCallback(Reconnecting)
            connection.connection = bootstrap connect connection.serverAddress
            connection.connection.awaitUninterruptibly
            log.error(connection.connection.getCause, "Reconnection to [%s:%d] has failed!".format(config.host, config.port))
          }
        }, config.reconnectDelay.length, config.reconnectDelay.unit)
      }
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      try {
        val ch = e.getChannel
        val msg = e.getMessage.asInstanceOf[String]
        log debug "Message received: %s".format(msg)
        msg match {
          case AuthenticationFailureResponse(error) => {
            log error error
            throw new UnauthorizedException(error)
          }
          case StreamResponse(id, from) => {
            ch.write(<handshake>{connection.hexCredentials(id)}</handshake>.toString)
          }
          case s => XML.loadString(s) match {
            case <handshake /> => {
              log info "Established connection to [%s:%d]".format(config.host, config.port)
              xmlProcessor foreach { _ ! RegisterSendCallback(nodes => e.getChannel.write(nodes.toString))}
              connection.notifyCallback(Connected)
            }
            case x: NodeSeq => xmlProcessor.foreach(a => a ! x )
            case _ =>  //componentConfig.xmlProcessor foreach { _ ! x }
          }
        }
      } catch {
        case e: Exception => {
          log.error(e, "Unexpected exception in component connection [%s]" format connection.address)
        }
      }

    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
      println(e.getCause, "There was a problem in the XMPP Channel Handler")
      e.getChannel.close
    }
  }
}
//class ComponentConnection(connectionConfig: ConnectionConfig) extends Actor {
//  import connectionConfig._
//
//
//
//}