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
    case class Send(xml: NodeSeq) extends ScapuletHandlerMessage
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

    protected def handleStanza: Receive = {
      case x: NodeSeq if handler.handleStanza.isDefinedAt(x) ⇒ {
        handler.lastStanza = x
        handler.handleStanza(x)
      }
    }

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
      case Send(response) ⇒ {
        context.parent ! response
      }
    }

    private def respondsTo(m: ScapuletHandlerRequest) =
      handler.serviceDiscoveryAware && serviceDiscoveryQueries.isDefinedAt(m)

    private val serviceDiscoveryQueries: PartialFunction[Any, Any] = {
      case Features   ⇒ handler.features
      case Identities ⇒ handler.identities
      case Infos      ⇒ handler.infos
      case ServerInfos ⇒ handler match {
        case h: ServerStanzaHandler ⇒ h.serverInfos
        case _                      ⇒ NodeSeq.Empty
      }
      //      case Items ⇒ sender ! handler.items
    }

  }
}

trait ServerInfos { self: StanzaHandler ⇒

  def serverInfos: NodeSeq = features map (_.toXml)
  def componentInfos: NodeSeq
}

trait ComponentHandler { self: StanzaHandler ⇒
  protected def componentConfig: ComponentConfig
  protected def component = system.scapulet.component(componentConfig.id)
  protected val me = componentConfig.connection.address

}

abstract class StanzaHandler(val handlerId: String)(implicit protected val system: ActorSystem) extends ErrorReply with ReplyMethods with Logging {

  import StanzaHandler.Messages._
  private[scapulet] implicit var actor: ActorRef = _

  def features: Seq[Feature]
  def identities: Seq[Identity]

  def infos: NodeSeq = NodeSeq.fromSeq((identities map (_.toXml)) ++ (features map (_.toXml)))

  private[scapulet] var lastStanza = NodeSeq.Empty

  protected def replyWith(msg: ⇒ NodeSeq) = {
    val m: NodeSeq = msg
    if (!(m forall (_.isEmpty))) {
      logger debug "Replying through %s with:\n%s".format(actor, m)
      actor ! Send(m)
    }
  }

  protected def safeReplyWith(msg: ⇒ NodeSeq): Unit = safeReplyWith(None)(msg)
  protected def safeReplyWith(include: Option[Elem])(msg: ⇒ NodeSeq) = {
    replyWith {
      try {
        msg
      } catch {
        case e ⇒ {
          val to = (lastStanza \ "@from").text
          val from = (lastStanza \ "@to").text
          logger.error(e, "There was an error when generating the reply for [%s] from [%s]", to, from)
          internalServerError(to, from, include)
        }
      }
    }
  }

  def handleStanza: Receive

  def handleMeta(sender: ⇒ ActorRef): Receive = { case "alwaysfail" ⇒ throw new UnsupportedOperationException("Received an impossible message of 'alwaysfail'") }

  def serviceDiscoveryAware = true

  protected val me: String
}

abstract class ServerStanzaHandler(handlerId: String)(implicit system: ActorSystem) extends StanzaHandler(handlerId) with ServerInfos {

}

