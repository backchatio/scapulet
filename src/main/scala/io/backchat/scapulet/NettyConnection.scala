package io.backchat.scapulet

import xml._
import _root_.org.jboss.netty.buffer.ChannelBuffers
import _root_.org.jboss.netty.channel._
import java.util.concurrent.atomic.AtomicInteger
import _root_.org.jboss.netty.bootstrap.{ ServerBootstrap, ClientBootstrap, Bootstrap }
import java.util.concurrent.{ TimeUnit, ThreadFactory, Executors }
import akka.actor.{ ActorSystem, ActorContext, ActorRef }
import socket.nio.{ NioServerSocketChannelFactory, NioClientSocketChannelFactory }

object NettyConnection {
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
private[scapulet] abstract class NettyConnection(connectionType: String, config: ConnectionConfig)(implicit actor: ActorRef, protected val system: ActorSystem) extends Logging {

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
    if (isConnected) doDisconnect().awaitUninterruptibly(3, TimeUnit.SECONDS)
  }

  def close() {
    connection.close().awaitUninterruptibly()
    bootstrap.releaseExternalResources()
    quiet { worker.awaitTermination(5, TimeUnit.SECONDS) }
    quiet { boss.awaitTermination(5, TimeUnit.SECONDS) }
  }

  private def quiet(thunk: ⇒ Any) = try { thunk } catch { case _ ⇒ }

  def write(xml: NodeSeq, channel: Channel = connection) {
    val txt = xml.map(Utility.trimProper _).toString
    val buff = ChannelBuffers.copiedBuffer(txt, Utf8)
    val writeFuture = channel.write(buff)
    writeFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
          logger error "Failed to send: %s".format(xml)
        }
      }
    })
  }

  private def internalConnect(phase: String) { connection = doConnect(phase) }
  protected def doConnect(phase: String): Channel
  protected def doDisconnect(): ChannelFuture

}

private[scapulet] class NettyServer(config: ConnectionConfig, pipeline: ⇒ ChannelPipeline)(implicit actor: ActorRef, system: ActorSystem)
    extends NettyConnection("server", config) {

  protected val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(boss, worker))
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

  protected def doDisconnect() = {
    logger info "Stopping XMPP server at [%s:%s]".format(config.host, config.port)
    connection.unbind()
  }

}

private[scapulet] class NettyClient(config: ConnectionConfig, pipeline: ⇒ ChannelPipeline)(implicit actor: ActorRef, system: ActorSystem)
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
    logger debug "The connection succeeded? %s".format(connectionFuture.isDone)
    if (connectionFuture.isCancelled) {
      logger info "Connection cancelled by user, exiting."
      system stop actor
    }
    if (!connectionFuture.isSuccess) {
      logger.error(connectionFuture.getCause, "XMPP connection has failed, trying to reconnect in %s seconds.".format(config.reconnectDelay.toSeconds))
      throw connectionFuture.getCause
    }
    connectionFuture.getChannel
  }

  protected def doDisconnect() = {
    logger info "Disconnecting from XMPP server at [%s:%s]".format(config.host, config.port)
    connection.disconnect()
  }

}
