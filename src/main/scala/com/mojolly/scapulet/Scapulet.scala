package com.mojolly.scapulet

import se.scalablesolutions.akka.actor.{Actor, ActorRef}
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.config.OneForOneStrategy
import com.mojolly.scapulet.ComponentConnection.FaultTolerantComponentConnection
import com.mojolly.scapulet.ComponentConnection.Messages._
import se.scalablesolutions.akka.util.Duration
import java.util.concurrent.TimeUnit
import xml.NodeSeq

/**
 * Created by IntelliJ IDEA.
 * User: ivan
 * Date: Aug 16, 2010
 * Time: 10:27:29 PM
 * 
 */

object Scapulet {

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
  case class ComponentConfig(xmlProcessor: ActorRef) extends NotNull


  sealed trait ScapuletMessage
  case class RegisterHandler(module: ActorRef)
  case class RegisteredHandler(module: ActorRef)
  case class UnregisterHandler(module: ActorRef)
  case class UnregisteredHandler(module: ActorRef)
  case class RegisterSendCallback(callback: NodeSeq => Unit)
  case class UnregisterSendCallback(callback: NodeSeq => Unit)
  case class SendFailed(stanza: NodeSeq)

  def makeComponentConnection(connectionConfig: ConnectionConfig) = {
    val conn = new FaultTolerantComponentConnection(connectionConfig)
    val comp = actorOf(new ScapuletComponent(conn))
    supervisor startLink comp
    comp
  }

//
//  def newConnection(connectionConfig: ConnectionConfig) = {
//    val conn = new FaultTolerantComponentConnection(connectionConfig)
//    conn.connect
//    conn
//  }

  class ScapuletSupervisor extends Actor {
    import self._
    faultHandler = Some(OneForOneStrategy(5, 5000))
    trapExit = List(classOf[Throwable])

    def receive = {
      case _ => {}
    }
  }

  private val supervisor = actorOf[ScapuletSupervisor].start

  def shutdownAll = supervisor.shutdownLinkedActors

}