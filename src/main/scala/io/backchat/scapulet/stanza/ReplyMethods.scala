package io.backchat.scapulet
package stanza

import xml._

trait ReplyMethods {
  self: StanzaHandler ⇒

  import CoreExt._

  /**
   * Creates an info query reply stanza, additional content can be added in the curry function
   *
   * @param ns The namespace for this reply
   * @param content The child nodes for this stanza
   *
   * @return a NodeSeq representing the stanza
   */
  protected def iqReply[TNode <: NodeSeq](ns: String)(content: ⇒ NodeSeq) = {
    <iq type="result" id={ (lastStanza \ "@id").text } to={ (lastStanza \ "from").text } from={ (lastStanza \ "to").text }>
      <query xmlns={ ns }>
        { content }
      </query>
    </iq>
  }

  /**
   * Creates a presence stanza
   *
   * @param presType An optional presence type if none is given it's assumed to be 'available'
   * @param children The child nodes for this stanza
   *
   * @return a NodeSeq representing the presence stanza
   */
  protected def presence(presType: Option[String] = None)(children: Seq[Node]) = {
    val ele = <presence to={ (lastStanza \ "from").text } from={ (lastStanza \ "to").text }>
                { children }
              </presence>
    if (presType.isDefined) {
      (presType map {
        pt ⇒ ele % Map("type" -> pt)
      }).get
    } else ele

  }

}