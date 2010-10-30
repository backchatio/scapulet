package com.mojolly.scapulet.stanza

import xml._

object DiscoInfoQuery {
  val DISCO_INFO_NS = "http://jabber.org/protocol/disco#info"

  val sl = <query xmlns={DISCO_INFO_NS} />

  def apply[TNode <: NodeSeq](id: String, from: String, to: String)(content: Seq[TNode]): NodeSeq = {
    <iq type="get" id={id} to={to} from={from}>
      <query xmlns={DISCO_INFO_NS}>
        { content }
      </query>
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
              if(node.isEmpty) None else Option(node.text)))
        } else None
      }
      case _ => None
    }
  }
}