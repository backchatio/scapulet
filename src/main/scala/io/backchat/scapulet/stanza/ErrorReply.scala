package io.backchat.scapulet
package stanza

import xml.{ NodeSeq, Elem }
import extractors.{InfoQuery, IQ}

trait ErrorReply {
  self: StanzaHandler ⇒

  protected def noNicknameSpecified(includes: Option[Elem] = None) =
    error(includes, "modify", <jid-malformed xmlns={ ns.Stanza }/>)

  protected def itemNotFound(includes: Option[Elem] = None) =
    error(includes, "cancel", <item-not-found xmlns={ ns.Stanza }/>)

  protected def conflict(includes: Option[Elem] = None) =
    error(includes, "cancel", <conflict xmlns={ ns.Stanza }/>)

  protected def internalServerError(includes: Option[Elem] = None) =
    error(includes, "wait", <internal-server-error xmlns={ ns.Stanza }/>)

  protected def notImplemented(includes: Option[Elem] = None) =
    error(includes, "cancel", <feature-not-implemented xmlns={ ns.Stanza }/>)

  protected def forbidden(includes: Option[Elem] = None) =
    error(includes, "cancel", <not-allowed xmlns={ ns.Stanza }/>)
  
  protected def serviceUnavailable(includes: Option[Elem] = None) = 
    error(includes, "cancel", <service-unavailable xmlns={ ns.Stanza }/>)

  def jid(user: String, domain: String, resource: Option[String] = None) =
    "%s@%s%s".format(user, domain, resource.map("/" + _) getOrElse "")

  protected def error(includes: Option[Elem], errorType: String, error: NodeSeq) = {
    lastStanza match {
      case InfoQuery(_) => iqReply(ns.DiscoInfo) {
        <query xmlns={ ns.DiscoInfo }/>
        <error type={errorType}>
          {error}
        </error>
      }
      case Elem(_, "iq", _, _, _*) => NodeSeq.Empty
      case Elem(_, "presence", _, _, _*) =>
        <presence from={ to } to={ from } type="error">
          { includes getOrElse Nil }<error type={ errorType }>
                                      { error }
                                    </error>
        </presence>
      case Elem(_, "message", _, _, _*) => 
        <message from={ to } to={ from } type="error">
          { includes getOrElse Nil }<error type={ errorType }>
                                      { error }
                                    </error>
        </message>
      case _ => throw new ScapuletException("Not all stanza types have a matching error reply yet.")
    }
  }
  
    
}