package io.backchat.scapulet
package stanza

import xml.{ NodeSeq, Elem }

trait ErrorReply {
  self: ScapuletHandler ⇒

  protected def noNicknameSpecified(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "modify", <jid-malformed xmlns={ ns.Stanza }/>)

  protected def itemNotFound(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <item-not-found xmlns={ ns.Stanza }/>)

  protected def conflict(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <conflict xmlns={ ns.Stanza }/>)

  protected def internalServerError(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "wait", <internal-server-error xmlns={ ns.Stanza }/>)

  protected def notImplemented(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <feature-not-implemented xmlns={ ns.Stanza }/>)

  protected def forbidden(from: String, to: String, includes: Option[Elem] = None) =
    error(from, to, includes, "cancel", <not-allowed xmlns={ ns.Stanza }/>)

  def jid(user: String, domain: String, resource: Option[String] = None) =
    "%s@%s%s".format(user, domain, resource.map("/" + _) getOrElse "")

  protected def error(from: String, to: String, includes: Option[Elem], errorType: String, error: NodeSeq) =
    <presence from={ to } to={ from } type="error">
      { includes getOrElse Nil }<error type={ errorType }>
                                  { error }
                                </error>
    </presence>
}