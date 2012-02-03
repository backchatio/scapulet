package io.backchat.scapulet

import akka.util.duration._
import xml.{Node, NodeSeq}
import akka.actor.ActorRef
import akka.util.Duration
import java.net.InetSocketAddress

object Scapulet {

  case class ConnectionConfig(
                               userName: String,
                               password: String,
                               host: String,
                               port: Int,
                               virtualHost: Option[String] = None,
                               connectionTimeout: Duration = 10 seconds,
                               reconnectDelay: Duration = 5 seconds,
                               connectionCallback: Option[ActorRef] = None,
                               maxThreads: Int = 25) extends NotNull {
    def domain = virtualHost getOrElse host

    def address = "%s.%s".format(userName, domain)

    def asHexSecret(id: String) = StringUtil.hash("%s%s".format(id, password))

    def socketAddress = new InetSocketAddress(host, port)
  }

  case class TLSConfig(
                        keystorePath: String = "",
                        keystoreType: String = "jks",
                        truststorePath: String = "",
                        truststoreType: String = "jks",
                        truststorePassword: String = "changeit",
                        pkcs11Library: String = "pkcs11.config",
                        verifyChain: Boolean = false,
                        verifyRootCA: Boolean = false,
                        verifyMatchingDomain: Boolean = false,
                        allowExpiredCertificates: Boolean = true,
                        allowSelfSignedCertificate: Boolean = false
                        )

  case class ClientConfig(
                           connectionConfig: ConnectionConfig,
                           tlsConfig: TLSConfig,
                           useTLS: Boolean = true,
                           useSASL: Boolean = true,
                           useCompression: Boolean = true,
                           sendPresence: Boolean = true,
                           loadRosterAtStartup: Boolean = false
                           ) extends NotNull {
    def domain = connectionConfig.virtualHost getOrElse connectionConfig.host

    def address = "%s@%s".format(connectionConfig.userName, domain)

    def asHexSecret(id: String) = StringUtil.hash("%s%s".format(id, connectionConfig.password))

    def socketAddress = connectionConfig.socketAddress
  }

  //  case class ComponentConfig(connectionConfig: ConnectionConfig, ) extends NotNull

  trait ScapuletConnection {
    def write(stanza: Seq[Node]): Unit
  }

  sealed trait ScapuletMessage

  case class RegisterHandler(module: ActorRef) extends ScapuletMessage

  case class RegisteredHandler(module: ActorRef) extends ScapuletMessage

  case class UnregisterHandler(module: ActorRef) extends ScapuletMessage

  case class UnregisteredHandler(module: ActorRef) extends ScapuletMessage

  sealed trait ComponentConnectionMessage

  case object Connect extends ComponentConnectionMessage

  case object Connected extends ComponentConnectionMessage

  case object Reconnecting extends ComponentConnectionMessage

  case object Disconnected extends ComponentConnectionMessage

  case object Disconnecting extends ComponentConnectionMessage

  case object Disconnect extends ComponentConnectionMessage

  case class Failure(cause: Throwable) extends ComponentConnectionMessage

  case class ConnectionShutdown(cause: Throwable) extends ComponentConnectionMessage

  case class Send(xml: Seq[Node]) extends ComponentConnectionMessage

  case class SendFailed(stanza: NodeSeq) extends ComponentConnectionMessage

  case class Sent(xml: NodeSeq) extends ComponentConnectionMessage
//
//  def makeComponentConnection(connectionConfig: ConnectionConfig) = {
//    val conn = new FaultTolerantComponentConnection(connectionConfig)
//    val comp = actorOf(new ScapuletComponent(conn))
//    supervisor startLink comp
//    (comp, conn)
//  }
//
//  def makeClientConnection(connectionConfig: ClientConfig) = {
//
//  }


//  class ScapuletSupervisor extends Actor {
//
//    import self._
//
//    faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)
//
//    def receive = {
//      case _ => {}
//    }
//  }

//  private[scapulet] val supervisor = actorOf[ScapuletSupervisor].start
//
//  def shutdownAll = supervisor.shutdownLinkedActors

  def systemInfo: String = {
    "%s-%s-%s, %s-%s %s".format(
      System.getProperty("os.name"),
      System.getProperty("os.arch"),
      System.getProperty("os.version"),
      System.getProperty("java.vm.name"),
      System.getProperty("java.version"),
      System.getProperty("java.vm.vendor")
    )
  }


}
