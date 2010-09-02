package com.mojolly.scapulet

import se.scalablesolutions.akka.config.ScalaConfig._
import com.mojolly.scapulet.Scapulet.{Send, UnregisterHandler, RegisterHandler, XMPP_STANZAS_NS}
import se.scalablesolutions.akka.actor.{ActorRegistry, Actor}
import xml.{NodeSeq, Elem, Node}

trait ScapuletHandler { this: Actor =>

  self.lifeCycle = Some(LifeCycle(Permanent))

//  override def init = {
//   self.supervisor foreach { _ ! RegisterHandler(self) }
//  }

  override def shutdown = {
    self.supervisor foreach { _ ! UnregisterHandler(self)}
  }

  protected def replyWith(msg: => Seq[Node]) = {
    val m: Seq[Node] = msg
    if(!m.isEmpty) {
      log debug "Replying with:"
      log debug m.map(_.toString).mkString("\n")
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

  protected def receive = handleStanza

  protected def noNicknameSpecified(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "modify", <jid-malformed xmlns={XMPP_STANZAS_NS} />)

  protected def itemNotFound(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <item-not-found xmlns={XMPP_STANZAS_NS} />)

  protected def conflict(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <conflict xmlns={XMPP_STANZAS_NS} />)

  protected def internalServerError(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "wait", <internal-server-error xmlns={XMPP_STANZAS_NS} />)

  protected def notImplemented(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <feature-not-implemented xmlns={XMPP_STANZAS_NS} />)


  protected def forbidden(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <not-allowed xmlns={XMPP_STANZAS_NS} />)

  def jid(user: String, domain: String, resource: Option[String] = None) =
      "%s@%s%s".format(user, domain, resource.map("/" + _) getOrElse "")


  protected def error(from: String, to: String, includes: Option[Elem], errorType: String, error: NodeSeq) =
    <presence from={to} to={from} type="error">
      { includes getOrElse Nil }
      <error type={errorType}>
        {error}
       </error>
    </presence>

}