package com.mojolly.scapulet.stanza

import xml._

object DiscoItemQuery {
  val DISCO_ITEMS_NS = "http://jabber.org/protocol/disco#items"
  def unapply(msg: Node) = {
    msg match {
      case <iq>{ c @ _* }</iq> if ((msg \ "@type").text == "get") => {
        if(c.exists {
          case Elem(_, "query", _, NamespaceBinding(_, `DISCO_ITEMS_NS`, _), _*) => true
          case _ => false }) {

          val id = msg \ "@id"
          val node = msg \ "query" \ "@node"
          val from = msg \ "@from"
          val to = msg \ "@to"
          Some((
              from.text,
              to.text,
              id.text,
              if (node.isEmpty) None else Option(node.text)))
        } else {
          None
        }

      }
      case _ => None
    }
  }
}

