package io.backchat.scapulet
package stanza

import xml.{ Elem, Node, NodeSeq }

object Feature {

  import CoreExt._

  def apply(name: String): NodeSeq = {
    require(name.isNotBlank, "The name cannot be blank")
    <feature var={ name }/>
  }

  def unapply(elem: Node) = elem match {
    case feat @ Elem(_, "feature", _, _, _*) if feat.attribute("var").isDefined =>
      feat.attribute("var") flatMap {
        a => Option(a.text)
      }
    case _ => None
  }
}