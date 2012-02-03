package com.mojolly.scapulet

import collection.JavaConversions._
import com.mojolly.scapulet.Scapulet._
import com.mojolly.scapulet.ComponentConnection.FaultTolerantComponentConnection
import akka.actor.Actor._
import akka.config._
import akka.config.Supervision._
import akka.actor.{Scheduler, ActorRef, Actor}
import java.util.concurrent.{TimeUnit, ConcurrentSkipListSet}
import xml.Node

class ScapuletComponent(val connection: FaultTolerantComponentConnection, handlers: ConcurrentSkipListSet[ActorRef], callback: Option[ActorRef]) extends Actor {
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)
  self.lifeCycle = Permanent

  def this(connection: FaultTolerantComponentConnection) = this(connection, new ConcurrentSkipListSet[ActorRef], None)

  connection.xmlProcessor = self

  override def postRestart(cause: Throwable) = {
    connection.xmlProcessor = self
  }

  override def preRestart(cause: Throwable) = {
    connection.xmlProcessor = null
  }

  private def cleanup = {
    // wait for cleanup to complete
    val nodes = (Seq[Node]() /: handlers) { (n, h) =>
      ((h !! Disconnecting).as[Seq[Node]] getOrElse Seq[Node]()) ++ n
    }
    log debug "The nodes:\n%s".format(nodes)
    connection.sayGoodbye(nodes) { 
      handlers foreach { _.stop }
      handlers.clear
      self.sender foreach { _ ! Disconnected }
    }
  }

  protected def manageHandlers: Receive = {
    case RegisterHandler(m) => {
      self link m
      if(!m.isRunning) m.start
      handlers add m
      callback foreach { _ ! RegisteredHandler(m)}
    }
    case UnregisterHandler(m) => {
      self unlink m
      if(m.isRunning) m.stop
      handlers remove m
      callback foreach { _ ! UnregisteredHandler(m) }
    }
  }

  protected def manageConnection: Receive = {
    case Connect => {
      connection.connect
    }
    case Disconnect => {
      cleanup      
    }
    case Send(xml) => connection.write(xml)
  }

  protected def dispatchToHandlers: Receive = {
    case a if (handlers.exists(_.isDefinedAt(a))) => {
      handlers.filter(_.isDefinedAt(a)) foreach { mod =>
        if(senderDefined_?) mod.forward(a)(someSelf)
        else mod.!(a)(None)
      }
    }
  }

  private def senderDefined_? = self.senderFuture.isDefined || self.sender.isDefined

  override def receive = manageConnection orElse dispatchToHandlers orElse manageHandlers
  override def isDefinedAt(msg: Any) = (manageConnection.isDefinedAt(msg) ||
      (handlers exists { _ isDefinedAt msg }) ||
      manageHandlers.isDefinedAt(msg))
}
object ScapuletComponent {
  
  def apply(connectionConfig: ConnectionConfig, supervisor: ActorRef = Scapulet.supervisor): ActorRef = {
    val conn = new FaultTolerantComponentConnection(connectionConfig)
    val comp = actorOf(new ScapuletComponent(conn))
    supervisor startLink comp
    comp
  }
}
