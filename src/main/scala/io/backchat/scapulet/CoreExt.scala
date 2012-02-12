package io.backchat.scapulet

import _root_.org.jboss.netty.handler.codec.base64.Base64
import xml._
import _root_.org.jboss.netty.buffer.ChannelBuffers

object CoreExt {

  class AllowAddingAttributes(elem: Elem) {
    def %(attrs: Map[String, String]) = {
      (elem /: (attrs map {
        case (k, v) ⇒ new UnprefixedAttribute(k, v, Null)
      }))(_ % _)
    }
  }

  class JidString(s: String) {
    def bareJid = s match {
      case JID(bareJid, _) ⇒ Some(bareJid)
      case _               ⇒ None
    }

    def resource = s match {
      case JID(_, Some(res)) ⇒ Some(res)
      case _                 ⇒ None
    }

    def hasResource = resource.isDefined

    def hasBareJid = bareJid.isDefined

    private[scapulet] def blank = s == null || s.trim.isEmpty

    private[scapulet] def nonBlank = s != null && !s.trim.isEmpty

    private[scapulet] def blankOpt = if (s == null || s.trim.isEmpty) None else Some(s)

    private[scapulet] def asBase64 = {
      Base64.encode(ChannelBuffers.copiedBuffer(s.getBytes(Utf8))).array()
    }

    private[scapulet] def base64Decoded = {
      Base64.decode(ChannelBuffers.copiedBuffer(s.getBytes(Utf8))).array()
    }

    private[scapulet] def isNotNull = s != null

    def sha1Hex = StringUtil.hash(s)

  }

  class ScapuletByteArray(arr: Array[Byte]) {
    def base64Encoded = Base64.encode(ChannelBuffers.copiedBuffer(arr)).array()
    def asBase64String = new String(base64Encoded, Utf8)
  }

}