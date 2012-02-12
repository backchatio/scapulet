package io.backchat.scapulet

import Scapulet._
import stanza._
import akka.actor._
import xml.{ NodeSeq, Elem, Node }

trait ScapuletHandler extends ScapuletConnectionActor with ErrorReply with ReplyMethods {

  protected def replyWith(msg: ⇒ NodeSeq) = {
    val m: NodeSeq = msg
    if (!m.isEmpty) {
      logger debug "Replying with:\n%s".format(m.map(_.toString).mkString("\n"))
      context.parent ! Send(m)
    }
  }

  protected def safeReplyWith(from: String, to: String, include: Option[Elem] = None)(msg: ⇒ Seq[Node]) = {
    replyWith {
      try {
        msg
      } catch {
        case e ⇒ {
          logger.error(e, "There was an error when generating the reply for [%s] from [%s]", from, to)
          internalServerError(from, to, include)
        }
      }
    }
  }

  protected def handleStanza: Receive

  protected def shuttingDown: Receive = {
    case Disconnecting ⇒ {
      sender ! NodeSeq.Empty
    }
    case Connected | Reconnecting ⇒ {
      sender ! NodeSeq.Empty
    }
  }

  protected def receive = handleStanza orElse shuttingDown

}
