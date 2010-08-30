package com.mojolly.scapulet

import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.config.OneForOneStrategy
import com.mojolly.scapulet.ComponentConnection.FaultTolerantComponentConnection
import se.scalablesolutions.akka.util.Duration
import java.util.concurrent.TimeUnit
import xml.{Node, NodeSeq}

object Scapulet {

  val XMPP_STANZAS_NS = "urn:ietf:params:xml:ns:xmpp-stanzas"

  object JID {
    def apply(bareJid: String, resource: String) = "%s/%s".format(bareJid, resource)

    def unapply(jid: String) = if(jid.contains("/")) {
      val parts = jid.split("/")
      Some((parts.head, parts.lastOption))
    } else Some((jid, None))
  }
  case class ConnectionConfig(
      userName: String,
      password: String,
      host: String,
      port: Int,
      virtualHost: Option[String] = None,
      connectionTimeout: Duration = Duration(10, TimeUnit.SECONDS),
      reconnectDelay: Duration = Duration(5, TimeUnit.SECONDS),
      connectionCallback: Option[ActorRef] = None) extends NotNull {
    def domain = virtualHost getOrElse host
    def address = "%s.%s".format(userName, domain)
    def asHexSecret(id: String) = StringUtil.hash(id + password)
  }
//  case class ComponentConfig(connectionConfig: ConnectionConfig, ) extends NotNull


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
    comp
  }


  class ScapuletSupervisor extends Actor {
    import self._
    faultHandler = Some(OneForOneStrategy(5, 5000))
    trapExit = List(classOf[Throwable])

    def receive = {
      case _ => {}
    }
  }

  private[scapulet] val supervisor = actorOf[ScapuletSupervisor].start

  def shutdownAll = supervisor.shutdownLinkedActors

}