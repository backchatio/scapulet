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
}