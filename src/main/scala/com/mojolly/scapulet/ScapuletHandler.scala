package com.mojolly.scapulet

import se.scalablesolutions.akka.actor.Actor
import se.scalablesolutions.akka.config.ScalaConfig._
import com.mojolly.scapulet.Scapulet.{UnregisterHandler, RegisterHandler}
import com.mojolly.scapulet.ComponentConnection.Messages._
import xml.NodeSeq

trait ScapuletHandler { this: Actor =>

  self.lifeCycle = Some(LifeCycle(Permanent))

  override def init = {
    self.supervisor foreach { _ ! RegisterHandler(self) }
  }

  override def shutdown = {
    self.supervisor foreach { _ ! UnregisterHandler(self)}
  }

  protected def reply(msg: NodeSeq) = self.supervisor foreach { _ ! Send(msg) }

  protected def handleStanza: Receive

//  private def handleComponentMessage: Receive = {}

  protected def receive = handleStanza //orElse handleComponentMessage

}