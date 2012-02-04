package io.backchat

import java.nio.charset.Charset
import scapulet.CoreExt.{ AllowAddingAttributes, JidString }
import xml.Elem

package object scapulet {

  private[scapulet] val UTF_8 = "UTF-8"
  private[scapulet] val Utf8 = Charset.forName(UTF_8)
  private[scapulet] val OldDateFormat = "yyyyMMdd'T'HH:mm:ss"
  private[scapulet] val DateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

  object ns {
    val Stanza = "urn:ietf:params:xml:ns:xmpp-stanzas"
    val Stream = "http://etherx.jabber.org/streams"
    val Client = "jabber:client"
    val Muc = "http://jabber.org/protocol/muc"
    val MucUser = "http://jabber.org/protocol/muc#user"
    val Ping = "ping"
    val PingExtensions = "http://www.xmpp.org/extensions/xep-0199.html#ns"
    val XmppPing = "urn:xmpp:ping"
    val Tls = "urn:ietf:params:xml:ns:xmpp-tls"
    val Sasl = "urn:ietf:params:xml:ns:xmpp-sasl"
    val XmppStream = "urn:ietf:params:xml:ns:xmpp-streams"
    val Compression = "http ://jabber.org/features/compress"

    object component {
      val Accept = "jabber:component:accept"
      val Connect = "jabber:component:connect"
    }
  }

  private[scapulet] implicit def addAttributesFromMap(elem: Elem) = new AllowAddingAttributes(elem)

  implicit def string2jidString(s: String) = new JidString(s)

}