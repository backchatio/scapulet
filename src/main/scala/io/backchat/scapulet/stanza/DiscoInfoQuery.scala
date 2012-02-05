package io.backchat.scapulet
package stanza

import xml._

object DiscoInfoQuery {

  import CoreExt._

  val DISCO_INFO_NS = "http://jabber.org/protocol/disco#info"

  val sl = <query xmlns={ DISCO_INFO_NS }/>

  def apply[TNode <: NodeSeq](id: String, from: String, to: String): NodeSeq = {
    require(id.nonBlank, "You need to provide an id for this disco info query")
    require(from.nonBlank, "You need to provide a sender address for this disco info query")
    require(to.nonBlank, "You need to provide a recipient address for this disco info query")

    <iq type="get" id={ id } to={ to } from={ from }>
      { sl }
    </iq>
  }

  def unapply(msg: Node) = {
    msg match {
      case <iq>{ c @ _* }</iq> if ((msg \ "@type").text == "get") => {
        if (c exists {
          case Elem(_, "query", _, NamespaceBinding(_, `DISCO_INFO_NS`, _), _*) => true
          case _ => false
        }) {
          val id = msg \ "@id"
          val node = msg \ "query" \ "@node"
          val from = msg \ "@from"
          val to = msg \ "@to"
          Some((
            from.text,
            to.text,
            id.text,
            if (node.isEmpty) None else Option(node.text)))
        } else None
      }
      case _ => None
    }
  }
}