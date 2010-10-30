package com.mojolly.scapulet
package stanza

import xml._

object Identity {
  def apply(category: String, `type`: String, name: Option[String] = None): NodeSeq = {
    name match {
      case Some(nm) => <identity category={category} type={`type`} name={nm} />
      case _ => <identity category={category} type={`type`} />
    }
  }

  def unapply(elem: Node) = elem match {
    case identity @ Elem(_, "identity", _, _, _*) => {
      val name = identity.attributes("name").headOption flatMap { a => Option(a.text) }
      Some(((identity \ "@category").text, (identity \ "@type").text, name))
    }
    case _ => None
  }
}