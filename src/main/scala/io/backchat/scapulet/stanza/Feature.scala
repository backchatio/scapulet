package io.backchat.scapulet
package stanza

import xml.{ Elem, Node }

object Feature {

  def apply(name: String) = new Feature(name)

  def unapply(elem: Node) = elem match {
    case feat @ Elem(_, "feature", _, _, _*) if feat.attribute("var").isDefined ⇒ (feat \ "@var" text).blankOpt
    case _ ⇒ None
  }
}

class Feature(val name: String) extends Product1[String] {

  require(name.nonBlank, "The name cannot be blank")

  def toXml = <feature var={ name }/>

  override def equals(that: Any) = that match {
    case o: Feature ⇒ o.name == name
    case _          ⇒ false
  }

  val _1 = name

  def canEqual(that: Any) = that match {
    case o: Feature ⇒ true
    case _          ⇒ false
  }

  override def hashCode() = scala.runtime.ScalaRunTime._hashCode(this)
}

