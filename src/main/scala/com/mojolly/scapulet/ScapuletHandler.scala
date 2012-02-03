package com.mojolly.scapulet

import akka.config.Supervision._
import Scapulet._
import stanza._
import XMPPConstants._
import akka.actor.{ActorRegistry, Actor}
import xml.{NodeSeq, Elem, Node}

trait ScapuletHandler extends ErrorReply with ReplyMethods { this: Actor =>

  self.lifeCycle = Permanent

  protected def replyWith(msg: => Seq[Node]) = {
    val m: Seq[Node] = msg
    if(!m.isEmpty) {
      log debug "Replying with:\n%s".format(m.map(_.toString).mkString("\n"))
      self.supervisor foreach { _ ! Send(m) }
    }
  }

  protected def safeReplyWith(from: String, to: String, include: Option[Elem] = None)( msg: => Seq[Node]) = {
    replyWith {
      try {
        msg
      } catch {
        case e => {
          log.error(e, "There was an error when generating the reply for [%s] from [%s]", from, to)
          internalServerError(from, to, include)
        }
      }
    }
  }

  protected def handleStanza: Receive
  protected def shuttingDown: Receive = {
    case Disconnecting => {
//      self.supervisor foreach { _ ! UnregisterHandler(self)}
      self reply Seq[Node]()
    }
    case Connected | Reconnecting => {
      if (self.sender.isDefined || self.senderFuture.isDefined)
        self reply Seq[Node]()
    }
  }

  protected def receive = handleStanza orElse shuttingDown

}
