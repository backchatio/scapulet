package io.backchat.scapulet
package stanza

import xml._

/**
 * Represents a disco items query
 * It's implemented as an extractor
 */
object DiscoItemsQuery {

  import Implicits._

  val DISCO_ITEMS_NS = "http://jabber.org/protocol/disco#items"

  def apply[TNode <: NodeSeq](id: String, from: String, to: String)(content: Seq[TNode]): NodeSeq = {
    require(id.isNotBlank, "You need to provide an id for this disco items query")
    require(from.isNotBlank, "You need to provide a sender address for this disco items query")
    require(to.isNotBlank, "You need to provide a recipient address for this disco items query")

    <iq type="get" id={id} to={to} from={from}>
      <query xmlns={DISCO_ITEMS_NS}>
        {content}
      </query>
    </iq>
  }

  def unapply(msg: Node) = {
    msg match {
      case <iq>{ c @ _* }</iq> if ((msg \ "@type").text == "get") => {
        if (c.exists {
          case Elem(_, "query", _, NamespaceBinding(_, `DISCO_ITEMS_NS`, _), _*) => true
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
        } else {
          None
        }

      }
      case _ => None
    }
  }
}

