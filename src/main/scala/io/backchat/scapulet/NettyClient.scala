//package io.backchat.scapulet
//
//import xml._
//import org.jboss.netty.buffer.ChannelBuffers
//import akka.actor.{ActorContext, ActorRef}
//import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
//import org.jboss.netty.bootstrap.ClientBootstrap
//import java.util.concurrent.{ThreadFactory, Executors}
//import org.jboss.netty.channel._
//import java.util.concurrent.atomic.AtomicInteger
//
//private[scapulet] object NettyClient {
//  private val threadCounter = new AtomicInteger(0)
//  private def threadFactory(name: String) = new ThreadFactory {
//    def newThread(r: Runnable) = {
//      val t = new Thread(r)
//      t.setName("component-connection-%s-%d".format(name, threadCounter.incrementAndGet()))
//      if (t.isDaemon)
//        t.setDaemon(false)
//      if (t.getPriority != Thread.NORM_PRIORITY)
//        t.setPriority(Thread.NORM_PRIORITY)
//      t
//    }
//  }
//}
//private[scapulet] class NettyClient(config: ConnectionConfig, pipeline: => ChannelPipeline)(implicit actor: ActorRef, context: ActorContext) extends Logging {
//
//  protected implicit val system = context.system
//
//  private val threadFactory = NettyClient.threadFactory(actor.path.name)
//  private val worker = Executors.newCachedThreadPool(threadFactory)
//  private val boss = Executors.newCachedThreadPool(threadFactory)
//  private val channelFactory = new NioClientSocketChannelFactory(boss, worker)
//
//  private val bootstrap = new ClientBootstrap(channelFactory)
//
//  val serverAddress = config.socketAddress
//
//  bootstrap.setPipelineFactory(new ChannelPipelineFactory {
//    def getPipeline = pipeline
//  })
//  bootstrap.setOption("connectTimeoutMillis", config.connectionTimeout.toMillis)
//  bootstrap.setOption("tcpNoDelay", true)
//  bootstrap.setOption("keepAlive", true)
//
//  private var _connection: ChannelFuture = _
//
//  def connect() {
//    if (!isConnected) internalConnect("Connecting")
//  }
//
//  def reconnect() {
//    if (isConnected) _connection.getChannel.disconnect().awaitUninterruptibly()
//    internalConnect("Reconnecting")
//  }
//
//  def isConnected = _connection != null && !_connection.isDone
//
//  def disconnect() {
//    _connection.getChannel.close().awaitUninterruptibly()
//    bootstrap.releaseExternalResources()
//  }
//
//  def write(xml: NodeSeq) {
//    val txt = xml.map(Utility.trimProper _).toString
//    val buff = ChannelBuffers.copiedBuffer(txt, Utf8)
//    val writeFuture = _connection.getChannel.write(buff)
//    writeFuture.addListener(new ChannelFutureListener {
//      def operationComplete(future: ChannelFuture) {
//        if (!future.isSuccess) {
//          logger error "Failed to send: %s".format(xml)
//        }
//      }
//    })
//  }
//
//  private def internalConnect(phase: String) {
//    _connection = bootstrap connect serverAddress
//    logger info "%s to XMPP server at [%s:%s]".format(phase, config.host, config.port)
//    _connection.awaitUninterruptibly
//    logger debug "The succeeded? %s".format(_connection.isDone)
//    if (_connection.isCancelled) {
//      logger info "Connection cancelled by user, exiting."
//      context stop actor
//    }
//    if (!_connection.isSuccess) {
//      logger.error(_connection.getCause, "XMPP connection has failed, trying to reconnect in %s seconds.".format(config.reconnectDelay.toSeconds))
//    }
//  }
//
//}