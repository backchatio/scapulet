package io.backchat.scapulet
package stanza

import xml.{ Elem, Node }

/**
 * The ''var'' attribute of the <feature/> element within the ''http://jabber.org/protocol/disco#info'' namespace may
 * contain any namespace that is registered with [[http://www.xmpp.org/registrar/namespaces.html the XMPP Registrar]] as well as some additional values
 * that have been separately registered with the Registrar.
 */
class Feature(val name: String) extends Product1[String] {

  require(name.nonBlank, "The name cannot be blank")

  def toXml = <feature var={ name }/>

  override def equals(that: Any) = that match {
    case o: Feature ⇒ o.name == name
    case _          ⇒ false
  }

  val _1 = name

  def canEqual(that: Any) = that match {
    case o: Feature ⇒ true
    case _          ⇒ false
  }

  override def hashCode() = scala.runtime.ScalaRunTime._hashCode(this)
}

/**
 * Contains the extractor and factory method for features.
 * Also contains predefined feature objects
 * 
 * @see [[io.backchat.scapulet.stanza.Feature]]
 */
object Feature {

  def apply(name: String) = new Feature(name)

  def unapply(elem: Node) = elem match {
    case feat @ Elem(_, "feature", _, _, _*) if feat.attribute("var").isDefined ⇒ (feat \ "@var" text).blankOpt
    case _ ⇒ None
  }

  /**
   * Support for DNS SRV lookups of XMPP services.
   */
  object dnssrv extends Feature("dnssrv")

  /**
   * Support for Unicode characters, including in displayed text, JIDs, and passwords.
   */
  object fullunicode extends Feature("fullunicode")

  /**
   * Support for the ''groupchat 1.0'' protocol.
   */
  object `gc-1.0` extends Feature("gc-1.0")

  /**
   * Support for service discovery info queries [[http://www.xmpp.org/extensions/xep-0030.html XEP-0030: Service Discovery]]
   * XMPP namespace ''http://jabber.org/protocol/disco#info''
   */
  object discoInfo extends Feature(ns.DiscoInfo)

  /**
   * Support for service discovery item queries [[http://www.xmpp.org/extensions/xep-0030.html XEP-0030: Service Discovery]]
   * XMPP namespace ''http://jabber.org/protocol/disco#items''
   */
  object discoItems extends Feature(ns.DiscoItems)

  /**
   * Support for [[http://www.xmpp.org/extensions/xep-0199.html XEP-0199: XMPP Ping]]
   */
  object xmppPing extends Feature(ns.XmppPing)


  /**
   * Support for [[http://www.xmpp.org/extensions/xep-0199.html XEP-0199: XMPP Ping]]
   */
  object xmppReceipts extends Feature(ns.XmppReceipts)

  /**
   * Application supports the 'xml:lang' attribute as described in [[http://tools.ietf.org/html/rfc6120 RFC 6120]].
   */
  object xmllang extends Feature("xmllang")

}

