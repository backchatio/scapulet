package io.backchat.scapulet

import stanza.JID
import xml._

object Implicits {

  implicit def addAttributesFromMap(elem: Elem) = new AllowAddingAttributes(elem)

  class AllowAddingAttributes(elem: Elem) {
    def %(attrs: Map[String, String]) = {
      (elem /: (attrs map {
        case (k, v) => new UnprefixedAttribute(k, v, Null)
      }))(_ % _)
    }
  }

  implicit def string2jidString(s: String) = new JidString(s)

  class JidString(s: String) {
    def bareJid = s match {
      case JID(bareJid, _) => Some(bareJid)
      case _ => None
    }

    def resource = s match {
      case JID(_, Some(res)) => Some(res)
      case _ => None
    }

    def hasResource = resource.isDefined

    def hasBareJid = bareJid.isDefined

    def isBlank = s == null || s.trim.isEmpty

    def isNotBlank = s != null && !s.trim.isEmpty

    def isNotNull = s != null

  }

}