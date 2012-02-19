package io.backchat.scapulet
package extractors

import xml._

object ID extends AttributeExtractor("id")
object IQ {
  def unapply(msg: Node): Option[String] = msg match {
    case Elem(_, "iq", _, _, _*) if (msg \ "@type" text).nonBlank ⇒ Some(msg \ "@type" text)
    case _ ⇒ None
  }
}

object InfoQuery {
  def unapply(msg: Node): Option[Node] = msg match {
    case <iq>{ c @ _* }</iq> && IQ("get") if isQuery(c) ⇒ Some(msg)
    case _ ⇒ None
  }

  val discoInfoNs = ns.DiscoInfo

  private def isQuery(c: Seq[Node]) = c exists {
    case Elem(_, "query", _, NamespaceBinding(_, `discoInfoNs`, _), _*) ⇒ true
    case _ ⇒ false
  }
}

object NoInfoQueryNode {
  def unapply(msg: Node): Boolean = InfoQueryNode.unapply(msg).isEmpty
}

object InfoQueryNode {
  def unapply(msg: Node): Option[String] = msg match {
    case InfoQuery(_) if hasNode(msg) ⇒ (msg \ "query" \ "@node" text).blankOpt
    case _                            ⇒ None
  }

  private def hasNode(msg: Node) = {
    val n = (msg \ "query" \ "@node")
    n.nonEmpty && n.text.nonBlank
  }
}

class AttributeExtractor(key: String) {
  def unapply(msg: Node): Option[String] = msg.attribute(key) map (_.text)
}
class JidExtractor(key: String) extends AttributeExtractor(key)

class ComponentJidExtractor(key: String) extends JidExtractor(key) {
  override def unapply(msg: Node) = super.unapply(msg) filterNot (_ contains "@")
}
class BareJidExtractor(key: String) extends JidExtractor(key) {
  override def unapply(msg: Node) = super.unapply(msg) filterNot (_.hasResource)
}
object FromJid extends JidExtractor("from")
object ToJid extends JidExtractor("to")
object FromComponent extends ComponentJidExtractor("from")
object ToComponent extends ComponentJidExtractor("to")
object FromBareJid extends BareJidExtractor("from")
object ToBareJid extends BareJidExtractor("to")
