package com.mojolly.scapulet
package stanza

import xml._
import XMPPConstants._
import util.Implicits._
import collection.mutable.ArrayBuffer

object StreamErrors {
  sealed trait XMPPStreamError

  class StreamError(condition: String) extends XMPPStreamError {
    require(condition.isNotBlank, "You need to specify a condition")

    def apply( text: Option[String] = None, applicationCondition: Seq[Node] = Seq.empty) = {
      (<stream:error>
        {XML.loadString("<%s xmlns=\"%s\" />".format(condition, XMPP_STREAMS_NS))}
        {text.map(t => <text xmlns={XMPP_STREAMS_NS}>{t}</text>) getOrElse Nil}
        {applicationCondition}
      </stream:error>).map(Utility.trim(_)).theSeq.head
    }

    def unapply(stanza: Node) = stanza.map(Utility.trim(_)).theSeq.head match {
      case err @ <stream:error>{ ch @ _*}</stream:error> if !(err \\ condition).isEmpty => {
        val txt = (err \ "text").text
        val text = if(txt.isNotBlank) Some(txt) else None
        val appCond = ch.filterNot(n => (condition :: "text" :: "" :: Nil).contains(n.label))
        val c = if (appCond.isEmpty) Seq[Node]() else {
          val ArrayBuffer(ac) = appCond
          ac
        }
        Some((text, c))
      }
      case _ => None
    }
  }

  object BadFormat extends StreamError("bad-format")
  object BadNamespacePrefix extends StreamError("bad-namespace-prefix")
  object Conflict extends StreamError("conflict")
  object ConnectionTimeout extends StreamError("connection-timeout")
  object HostGone extends StreamError("host-gone")
  object HostUnknown extends StreamError("host-unknown")
  object ImproperAddressing extends StreamError("improper-addressing")
  object InternalServerError extends StreamError("internal-server-error")
  object InvalidForm extends StreamError("invalid-form")
  object InvalidId extends StreamError("invalid-id")
  object InvalidNamespace extends StreamError("invalid-namespace")
  object InvalidXml extends StreamError("invalid-xml")
  object NotAuthorized extends StreamError("not-authorized")
  object PolicyViolation extends StreamError("policy-violation")
  object RemoteConnectionFailed extends StreamError("remote-connection-failed")
  object ResourceConstraint extends StreamError("resource-constraint")
  object RestrictedXml extends StreamError("restricted-xml")
  object SeeOtherHost extends StreamError("see-other-host")
  object SystemShutdown extends StreamError("system-shutdown")
  object UndefinedCondition extends StreamError("undefined-condition")
  object UnsupportedEncoding extends StreamError("unsupported-encoding")
  object UnsupportedStanzaType extends StreamError("unsupported-stanza-type")
  object UnsupportedVersion extends StreamError("unsupported-version")
  object XmlNotWellFormed extends StreamError("xml-not-well-formed")

}