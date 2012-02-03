package io.backchat.scapulet

import Scapulet._
import stanza._
import akka.actor.{Actor}
import xml.{Elem, Node}

trait ScapuletHandler extends ErrorReply with ReplyMethods {
  this: Actor =>
  
  @transient protected lazy val logger = akka.event.Logging(context.system, this) 

  protected def replyWith(msg: => Seq[Node]) = {
    val m: Seq[Node] = msg
    if (!m.isEmpty) {
      logger debug "Replying with:\n%s".format(m.map(_.toString).mkString("\n"))
      context.parent ! Send(m)
    }
  }

  protected def safeReplyWith(from: String, to: String, include: Option[Elem] = None)(msg: => Seq[Node]) = {
    replyWith {
      try {
        msg
      } catch {
        case e => {
          logger.error(e, "There was an error when generating the reply for [%s] from [%s]", from, to)
          internalServerError(from, to, include)
        }
      }
    }
  }

  protected def handleStanza: Receive

  protected def shuttingDown: Receive = {
    case Disconnecting => {
      sender ! Seq.empty[Node]
    }
    case Connected | Reconnecting => {
      sender ! Seq.empty[Node]
    }
  }

  protected def receive = handleStanza orElse shuttingDown

}
