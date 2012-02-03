package com.mojolly.scapulet

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor._
import akka.config.Supervision.OneForOneStrategy
import com.mojolly.scapulet.ComponentConnection.FaultTolerantComponentConnection
import akka.util.Duration
import java.util.concurrent.TimeUnit
import xml.{Node, NodeSeq}

object Scapulet {

  case class ConnectionConfig(
      userName: String,
      password: String,
      host: String,
      port: Int,
      virtualHost: Option[String] = None,
      connectionTimeout: Duration = Duration(10, TimeUnit.SECONDS),
      reconnectDelay: Duration = Duration(5, TimeUnit.SECONDS),
      connectionCallback: Option[ActorRef] = None,
      maxThreads: Int = 25) extends NotNull {
    def domain = virtualHost getOrElse host
    def address = "%s.%s".format(userName, domain)
    def asHexSecret(id: String) = StringUtil.hash("%s%s".format(id, password))
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
  }
//  case class ComponentConfig(connectionConfig: ConnectionConfig, ) extends NotNull

  trait ScapuletConnection {
    def write(stanza: Seq[Node]) : Unit
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

  def makeComponentConnection(connectionConfig: ConnectionConfig) = {
    val conn = new FaultTolerantComponentConnection(connectionConfig)
    val comp = actorOf(new ScapuletComponent(conn))
    supervisor startLink comp
    (comp, conn)
  }

  def makeClientConnection(connectionConfig: ClientConfig) = {

  }


  class ScapuletSupervisor extends Actor {
    import self._
    faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)

    def receive = {
      case _ => {}
    }
  }

  private[scapulet] val supervisor = actorOf[ScapuletSupervisor].start

  def shutdownAll = supervisor.shutdownLinkedActors

  def systemInfo : String = {
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