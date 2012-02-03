package io.backchat.scapulet
package stanza

import xml._

trait ReplyMethods {
  self: ScapuletHandler =>

  import Implicits._


  /**
   * Creates an info query reply stanza, additional content can be added in the curry function
   *
   * @param ns The namespace for this reply
   * @param id The id for the info query message
   * @param from The sender of this stanza
   * @param to The recipient for this stanza
   * @param content The child nodes for this stanza
   *
   * @return a NodeSeq representing the stanza
   */
  protected def iqReply[TNode <: NodeSeq](ns: String, id: String, from: String, to: String)(content: Seq[TNode]) = {
    <iq type="result" id={id} to={to} from={from}>
      <query xmlns={ns}>
        {content}
      </query>
    </iq>
  }

  /**
   * Creates a presence stanza
   *
   * @param from The sender of this presence stanza
   * @param to The recipient of this presence stanza
   * @param presType An optional presence type if none is given it's assumed to be 'available'
   * @param children The child nodes for this stanza
   *
   * @return a NodeSeq representing the presence stanza
   */
  protected def presence(from: String, to: String, presType: Option[String])(children: Seq[Node]) = {
    val ele = <presence from={from} to={to}>
      {children}
    </presence>
    if (presType.isDefined) {
      (presType map {
        pt => ele % Map("type" -> pt)
      }).get
    } else ele

  }


}