package io.backchat.scapulet
package stanza

import xml.{NodeSeq, Elem}
import XMPPConstants._

trait ErrorReply {
  self: ScapuletHandler =>

  protected def noNicknameSpecified(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "modify", <jid-malformed xmlns={XMPP_STANZAS_NS}/>)

  protected def itemNotFound(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <item-not-found xmlns={XMPP_STANZAS_NS}/>)

  protected def conflict(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <conflict xmlns={XMPP_STANZAS_NS}/>)

  protected def internalServerError(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "wait", <internal-server-error xmlns={XMPP_STANZAS_NS}/>)

  protected def notImplemented(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <feature-not-implemented xmlns={XMPP_STANZAS_NS}/>)


  protected def forbidden(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <not-allowed xmlns={XMPP_STANZAS_NS}/>)

  def jid(user: String, domain: String, resource: Option[String] = None) =
    "%s@%s%s".format(user, domain, resource.map("/" + _) getOrElse "")


  protected def error(from: String, to: String, includes: Option[Elem], errorType: String, error: NodeSeq) =
    <presence from={to} to={from} type="error">
      {includes getOrElse Nil}<error type={errorType}>
      {error}
    </error>
    </presence>
}