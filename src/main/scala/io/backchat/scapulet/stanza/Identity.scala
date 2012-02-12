package io.backchat.scapulet
package stanza

import xml._

object Identity {

  def apply(category: String, `type`: String, name: Option[String] = None): Identity = {
    new Identity(category, `type`, name)
  }

  def unapply(elem: Node) = elem match {
    case identity @ Elem(_, "identity", _, _, _*) if (identity.attribute("category").isDefined && identity.attribute("type").isDefined) ⇒ {
      val name = identity.attribute("name") flatMap {
        a ⇒ Option(a.text)
      }
      Some(((identity \ "@category").text, (identity \ "@type").text, name))
    }
    case _ ⇒ None
  }
}

class Identity(val category: String, val `type`: String, val name: Option[String] = None) extends Product3[String, String, Option[String]] {

  require(category.nonBlank, "You have to provide a category")
  require(`type`.nonBlank, "You have to provide a type")

  val _1 = category

  val _2 = `type`

  val _3 = name

  def canEqual(that: Any) = that.isInstanceOf[Identity]

  def toXml = name match {
    case Some(nm) ⇒ <identity category={ category } type={ `type` } name={ nm }/>
    case _        ⇒ <identity category={ category } type={ `type` }/>
  }

  override def hashCode() = scala.runtime.ScalaRunTime._hashCode(this)

  override def equals(p1: Any) = canEqual(p1) && scala.runtime.ScalaRunTime._equals(this, p1)
}