package com.mojolly.scapulet
package stanza

import xml._
import util._

object DiscoItem {

  import Implicits._

  def apply(jid: String, name: Option[String], node: Option[String]) = {
    require(jid.isNotBlank, "You need to provide a jid for this disco item")
    val res = <item jid={jid}></item>
    var attrs = Map[String, String]()
    if (name.isDefined && name.get.isNotBlank) attrs += "name" -> name.get
    if (node.isDefined && node.get.isNotBlank) attrs += "node" -> node.get
    res % attrs
  }

  def unapply(elem: Node) = elem match {
    case it @ Elem(_, "item", _, _, _*) if it.attribute("jid").isDefined => {
      val name = it.attribute("name") flatMap { a => Option(a.text) }
      val node = it.attribute("node") flatMap { a => Option(a.text) }
      Some(((it \ "@jid").text, name, node))
    }
    case _ => None
  }
}