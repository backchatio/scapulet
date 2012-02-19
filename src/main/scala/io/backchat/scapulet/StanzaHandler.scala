package io.backchat.scapulet

import Scapulet._
import stanza._
import akka.actor._
import Actor.Receive
import xml.{ NodeSeq, Elem, Node }

object StanzaHandler {

  object Messages {
    sealed trait ScapuletHandlerMessage
    sealed trait ScapuletHandlerRequest
    case object Features extends ScapuletHandlerRequest
    case object Identities extends ScapuletHandlerRequest
    case object Infos extends ScapuletHandlerRequest
    case object Items extends ScapuletHandlerRequest
    case object ComponentInfos extends ScapuletHandlerRequest
    case object ServerInfos extends ScapuletHandlerRequest
    case class Request(request: ScapuletHandlerRequest, seenBy: Seq[ActorRef] = Seq.empty) extends ScapuletHandlerMessage
    case class Send(xml: Node) extends ScapuletHandlerMessage
    case class Register(handler: StanzaHandler) extends ScapuletHandlerMessage
    case class Unregister(handler: StanzaHandler) extends ScapuletHandlerMessage
  }

  private[scapulet] class ScapuletHandlerHost(handler: StanzaHandler) extends ScapuletConnectionActor {
    import Messages._

    override def preStart() {
      super.preStart()
      logger info ("Loading handler %s" format handler.handlerId)
    }

    protected def receive = internalQueries orElse handler.handleMeta(sender) orElse handler.handleStanza orElse shuttingDown

    protected def shuttingDown: Receive = {
      case Disconnecting ⇒ {
        sender ! NodeSeq.Empty
      }
      case Connected | Reconnecting ⇒ {
        sender ! NodeSeq.Empty
      }
    }

    protected[scapulet] def internalQueries: Receive = {
      case Request(m, seenBy) if respondsTo(m) ⇒ sender ! serviceDiscoveryQueries(m)
      case Send(response)                      ⇒ context.parent ! response
    }

    private def respondsTo(m: ScapuletHandlerRequest) =
      handler.serviceDiscoveryAware && serviceDiscoveryQueries.isDefinedAt(m)

    private val serviceDiscoveryQueries: PartialFunction[Any, Any] = {
      case Features       ⇒ handler.features
      case Identities     ⇒ handler.identities
      case ComponentInfos ⇒ handler.componentInfos
      case Infos          ⇒ handler.infos
      case ServerInfos ⇒ handler match {
        case h: ServerStanzaHandler ⇒ h.serverInfos
        case _                      ⇒ NodeSeq.Empty
      }
      //      case Items ⇒ sender ! handler.items
    }

  }
}

trait ServerStanzaHandler { self: StanzaHandler ⇒

  def serverInfos: NodeSeq = features map (_.toXml)
}

abstract class StanzaHandler(val handlerId: String)(implicit protected val system: ActorSystem) extends ErrorReply with ReplyMethods with Logging {

  import StanzaHandler.Messages._
  private[scapulet] implicit var actor: ActorRef = _
  def features: Seq[Feature]
  def identities: Seq[Identity]

  def componentInfos: NodeSeq = NodeSeq.fromSeq((identities map (_.toXml)) ++ (features map (_.toXml)))
  def infos: NodeSeq = NodeSeq.Empty

  protected def replyWith(msg: ⇒ Node) = {
    val m: Node = msg
    if (!m.isEmpty) {
      logger debug "Replying with:\n%s".format(m.map(_.toString).mkString("\n"))
      actor ! Send(m)
    }
  }

  protected def safeReplyWith(from: String, to: String, include: Option[Elem] = None)(msg: ⇒ Node) = {
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

  def handleMeta(sender: ⇒ ActorRef): Receive = { case "alwaysfail" ⇒ throw new UnsupportedOperationException("Received an impossible message of 'alwaysfail'") }

  def serviceDiscoveryAware = true

}

