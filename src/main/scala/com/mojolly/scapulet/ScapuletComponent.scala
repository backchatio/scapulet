package com.mojolly.scapulet

import java.util.concurrent.ConcurrentSkipListSet
import se.scalablesolutions.akka.actor.{ActorRef, Actor}
import collection.JavaConversions._
import com.mojolly.scapulet.Scapulet._
import com.mojolly.scapulet.ComponentConnection.FaultTolerantComponentConnection
import com.mojolly.scapulet.ComponentConnection.Messages._
import xml.NodeSeq

class ScapuletComponent(connection: FaultTolerantComponentConnection, handlers: ConcurrentSkipListSet[ActorRef], callback: Option[ActorRef]) extends Actor {


  def this(connection: FaultTolerantComponentConnection) = this(connection, new ConcurrentSkipListSet[ActorRef], None)

  connection.xmlProcessor = self
  private var _sendCallback: Option[NodeSeq => Unit] = None

  override def postRestart(cause: Throwable) = {
    connection.xmlProcessor = self
  }

  override def preRestart(cause: Throwable) = {
    connection.xmlProcessor = null
  }

  override def shutdown = {
    connection.disconnect
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
    case RegisterSendCallback(callback) => _sendCallback = Option(callback)
    case UnregisterSendCallback(_) => _sendCallback = None
    case Send(xml) => _sendCallback foreach { _(xml) }
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
  override def isDefinedAt(msg: Any) = (handlers exists { _ isDefinedAt msg }) || manageHandlers.isDefinedAt(msg)
}
object ScapuletComponent {

  def apply(connection: FaultTolerantComponentConnection, handlers: ConcurrentSkipListSet[ActorRef] = new ConcurrentSkipListSet[ActorRef], callback: Option[ActorRef] = None) =
    new ScapuletComponent(connection, handlers, callback)

}