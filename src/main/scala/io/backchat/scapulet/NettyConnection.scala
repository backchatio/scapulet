package io.backchat.scapulet

import xml._
import org.jboss.netty.buffer.ChannelBuffers
import akka.actor.{ ActorContext, ActorRef }
import org.jboss.netty.channel._
import java.util.concurrent.atomic.AtomicInteger
import socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.{ ServerBootstrap, ClientBootstrap, Bootstrap }
import java.util.concurrent.{TimeUnit, ThreadFactory, Executors}

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

  protected var connection: Channel = _

  def connect() {
    if (!isConnected) internalConnect("Connecting")
  }

  def reconnect() {
    disconnect()
    internalConnect("Reconnecting")
  }

  def isConnected = connection != null && connection.isConnected

  def disconnect() {
    if (isConnected) doDisconnect()
  }
  
  def close() {
    connection.close().awaitUninterruptibly()
    bootstrap.releaseExternalResources()
    quiet { worker.awaitTermination(5, TimeUnit.SECONDS) }
    quiet { boss.awaitTermination(5, TimeUnit.SECONDS) }
  }
  
  private def quiet(thunk: => Any) = try { thunk } catch { case _ => }

  def write(xml: NodeSeq) {
    val txt = xml.map(Utility.trimProper _).toString
    val buff = ChannelBuffers.copiedBuffer(txt, Utf8)
    val writeFuture = connection.write(buff)
    writeFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          logger error "Failed to send: %s".format(xml)
        }
      }
    })
  }

  private def internalConnect(phase: String) {
    connection = doConnect(phase)

  }

  protected def doConnect(phase: String): Channel
  protected def doDisconnect(): ChannelFuture

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

  protected def doDisconnect() = connection.unbind().awaitUninterruptibly(3, TimeUnit.SECONDS)
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

  protected def doDisconnect() = connection.disconnect().awaitUninterruptibly(3, TimeUnit.SECONDS)
}
