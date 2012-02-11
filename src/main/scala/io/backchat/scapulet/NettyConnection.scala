package io.backchat.scapulet

import xml._
import org.jboss.netty.buffer.ChannelBuffers
import akka.actor.{ ActorContext, ActorRef }
import java.util.concurrent.{ ThreadFactory, Executors }
import org.jboss.netty.channel._
import java.util.concurrent.atomic.AtomicInteger
import socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.{ ServerBootstrap, ClientBootstrap, Bootstrap }

private[scapulet] object NettyConnection {
  private val threadCounter = new AtomicInteger(0)
  private def threadFactory(connectionType: String, name: String) = new ThreadFactory {
    def newThread(r: Runnable) = {
      val t = new Thread(r)
      t.setName("%s-connection-%s-%d".format(connectionType, name, threadCounter.incrementAndGet()))
      if (t.isDaemon)
        t.setDaemon(false)
      if (t.getPriority != Thread.NORM_PRIORITY)
        t.setPriority(Thread.NORM_PRIORITY)
      t
    }
  }
}
private[scapulet] abstract class NettyConnection(connectionType: String, config: ConnectionConfig)(implicit actor: ActorRef, context: ActorContext) extends Logging {

  protected implicit val system = context.system
  protected val threadFactory = NettyConnection.threadFactory(connectionType, actor.path.name)
  protected val worker = Executors.newCachedThreadPool(threadFactory)
  protected val boss = Executors.newCachedThreadPool(threadFactory)
  protected val channelFactory = new NioClientSocketChannelFactory(boss, worker)

  protected def bootstrap: Bootstrap
  val serverAddress = config.socketAddress

  private var _connection: Channel = _

  def connect() {
    if (!isConnected) internalConnect("Connecting")
  }

  def reconnect() {
    if (isConnected) _connection.disconnect().awaitUninterruptibly()
    internalConnect("Reconnecting")
  }

  def isConnected = _connection != null && _connection.isConnected

  def disconnect() {
    _connection.close().awaitUninterruptibly()
    bootstrap.releaseExternalResources()
  }

  def write(xml: NodeSeq) {
    val txt = xml.map(Utility.trimProper _).toString
    val buff = ChannelBuffers.copiedBuffer(txt, Utf8)
    val writeFuture = _connection.write(buff)
    writeFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          logger error "Failed to send: %s".format(xml)
        }
      }
    })
  }

  private def internalConnect(phase: String) {
    _connection = doConnect(phase)

  }

  protected def doConnect(phase: String): Channel

}

private[scapulet] class NettyServer(config: ConnectionConfig, pipeline: ⇒ ChannelPipeline)(implicit actor: ActorRef, context: ActorContext)
    extends NettyConnection("server", config) {
  protected val bootstrap = new ServerBootstrap(channelFactory)
  bootstrap.setPipelineFactory(new ChannelPipelineFactory {
    def getPipeline = pipeline
  })
  bootstrap.setOption("tcpNoDelay", true)
  bootstrap.setOption("keepAlive", true)

  protected def doConnect(phase: String) = {
    val ch = bootstrap bind serverAddress
    logger info "Started XMPP server at [%s:%s]".format(phase, config.host, config.port)
    ch
  }
}

private[scapulet] class NettyClient(config: ConnectionConfig, pipeline: ⇒ ChannelPipeline)(implicit actor: ActorRef, context: ActorContext)
    extends NettyConnection("client", config) {
  protected val bootstrap = new ClientBootstrap(channelFactory)
  bootstrap.setPipelineFactory(new ChannelPipelineFactory {
    def getPipeline = pipeline
  })
  bootstrap.setOption("connectTimeoutMillis", config.connectionTimeout.toMillis)
  bootstrap.setOption("tcpNoDelay", true)
  bootstrap.setOption("keepAlive", true)

  protected def doConnect(phase: String) = {
    val connectionFuture = bootstrap connect serverAddress
    logger info "%s to XMPP server at [%s:%s]".format(phase, config.host, config.port)
    connectionFuture.awaitUninterruptibly
    logger debug "The succeeded? %s".format(connectionFuture.isDone)
    if (connectionFuture.isCancelled) {
      logger info "Connection cancelled by user, exiting."
      context stop actor
    }
    if (!connectionFuture.isSuccess) {
      logger.error(connectionFuture.getCause, "XMPP connection has failed, trying to reconnect in %s seconds.".format(config.reconnectDelay.toSeconds))
    }
    connectionFuture.getChannel
  }

}
