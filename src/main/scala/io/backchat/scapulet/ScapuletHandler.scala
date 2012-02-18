package io.backchat.scapulet

import Scapulet._
import stanza._
import akka.actor._
import Actor.Receive
import xml.{ NodeSeq, Elem, Node }

object ScapuletHandler {

  object Messages {
    sealed trait ScapuletHandlerMessage
    sealed trait ScapuletHandlerRequest extends ScapuletHandlerMessage
    case object Features extends ScapuletHandlerRequest
    case object Identities extends ScapuletHandlerRequest
    case object Infos extends ScapuletHandlerRequest
    case object Items extends ScapuletHandlerRequest
    case object ComponentInfos extends ScapuletHandlerRequest
    case object ServerInfos extends ScapuletHandlerRequest
    case class Register(handler: ScapuletHandler) extends ScapuletHandlerMessage
    case class Unregister(handler: ScapuletHandler) extends ScapuletHandlerMessage
  }

  private[scapulet] class ScapuletHandlerHost(handler: ScapuletHandler) extends ScapuletConnectionActor {
    import Messages._
    protected def receive = internalQueries orElse handler.handleMeta(sender) orElse handler.handleStanza orElse shuttingDown

    protected def shuttingDown: Receive = {
      case Disconnecting ⇒ {
        sender ! NodeSeq.Empty
      }
      case Connected | Reconnecting ⇒ {
        sender ! NodeSeq.Empty
      }
    }
    
    

    protected[scalatra] def internalQueries: Receive = {
      case m if handler.serviceDiscoveryAware && serviceDiscoveryQueries.isDefinedAt(m) => sender ! m
      case m: Send    ⇒ context.parent ! m
    }
    
    private val serviceDiscoveryQueries: Receive = {
      case Features   ⇒ sender ! handler.features
      case Identities ⇒ sender ! handler.identities
      case ComponentInfos => sender ! handler.componentInfos
      case Infos => sender ! handler.infos
      case ServerInfos => sender ! handler.serverInfos
      case Items => sender ! handler.items
    }
    

  }
}

abstract class ScapuletHandler(val handlerId: String)(implicit protected val system: ActorSystem) extends ErrorReply with ReplyMethods with Logging {

  import ScapuletHandler.Messages._
  private[scapulet] implicit var actor: ActorRef = _
  def features: Seq[Feature]
  def identities: Seq[Identity]
  
  def componentInfos: NodeSeq = (features map (_.toXml)) ++ (identities map (_.toXml))
  def items: NodeSeq = NodeSeq.Empty
  def infos: NodeSeq = NodeSeq.Empty
  def serverInfos: NodeSeq = NodeSeq.Empty

  protected def replyWith(msg: ⇒ NodeSeq) = {
    val m: NodeSeq = msg
    if (!m.isEmpty) {
      logger debug "Replying with:\n%s".format(m.map(_.toString).mkString("\n"))
      actor ! Send(m)
    }
  }

  protected def safeReplyWith(from: String, to: String, include: Option[Elem] = None)(msg: ⇒ NodeSeq) = {
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

  def handleMeta(sender: => ActorRef): Receive = { case "alwaysfail" => throw new UnsupportedOperationException("Received an impossible message of 'alwaysfail'") }
  
  def serviceDiscoveryAware = true
}



