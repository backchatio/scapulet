package io.backchat.scapulet

import Scapulet._
import stanza._
import akka.actor._
import Actor.Receive
import xml.{ NodeSeq, Elem, Node }

object ScapuletHandler {

  object Messages {
    sealed trait ScapuletHandlerMessage
    case object Features extends ScapuletHandlerMessage
    case object Identities extends ScapuletHandlerMessage
    case class Register(handler: ScapuletHandler) extends ScapuletHandlerMessage
    case class Unregister(handler: ScapuletHandler) extends ScapuletHandlerMessage
  }

  private[scapulet] class ScapuletHandlerHost(handler: ScapuletHandler) extends ScapuletConnectionActor {
    import Messages._
    protected def receive = internalQueries orElse handler.handleStanza orElse shuttingDown

    protected def shuttingDown: Receive = {
      case Disconnecting ⇒ {
        sender ! NodeSeq.Empty
      }
      case Connected | Reconnecting ⇒ {
        sender ! NodeSeq.Empty
      }
    }

    protected def internalQueries: Receive = {
      case Features   ⇒ sender ! handler.features
      case Identities ⇒ sender ! handler.identities
      case m: Send    ⇒ context.parent ! m
    }

  }
}

abstract class ScapuletHandler(val id: String)(implicit protected val system: ActorSystem) extends ErrorReply with ReplyMethods with Logging {

  private[scapulet] implicit var actor: ActorRef = _
  def features: Seq[Feature]
  def identities: Seq[Identity]

  protected def replyWith(msg: ⇒ NodeSeq) = {
    val m: NodeSeq = msg
    if (!m.isEmpty) {
      logger debug "Replying with:\n%s".format(m.map(_.toString).mkString("\n"))
      actor ! Send(m)
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

  def handleStanza: Receive

}
