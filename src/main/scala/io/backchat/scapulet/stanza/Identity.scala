package io.backchat.scapulet
package stanza

import xml._

object Identity {

  import Implicits._

  def apply(category: String, `type`: String, name: Option[String] = None): NodeSeq = {
    require(category.isNotBlank, "You have to provide a category")
    require(`type`.isNotBlank, "You have to provide a type")

    name match {
      case Some(nm) => <identity category={ category } type={ `type` } name={ nm }/>
      case _ => <identity category={ category } type={ `type` }/>
    }
  }

  def unapply(elem: Node) = elem match {
    case identity @ Elem(_, "identity", _, _, _*) if (identity.attribute("category").isDefined && identity.attribute("type").isDefined) => {
      val name = identity.attribute("name") flatMap {
        a => Option(a.text)
      }
      Some(((identity \ "@category").text, (identity \ "@type").text, name))
    }
    case _ => None
  }
}