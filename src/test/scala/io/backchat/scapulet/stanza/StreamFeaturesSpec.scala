package io.backchat.scapulet
package stanza

import xml._
import org.specs2.Specification

class StreamFeaturesSpec extends Specification {
  def is =

    "The stream features extractor should" ^
      "match a stanza with tls and sasl mechanisms and a register element" ! {
        StreamFeatures.unapply(fullStanza) must beSome((
          true,
          List("DIGEST-MD5", "PLAIN"),
          NodeSeq.fromSeq(<register xmlns="http://jabber.org/features/iq-register"/>.toSeq)))
      } ^
      end

  def fullStanza = <stream:features>
                     <starttls xmlns="urn:ietf:params:xml:ns:xmpp-tls"/>
                     <mechanisms xmlns="urn:ietf:params:xml:ns:xmpp-sasl">
                       <mechanism>DIGEST-MD5</mechanism>
                       <mechanism>PLAIN</mechanism>
                     </mechanisms>
                     <register xmlns="http://jabber.org/features/iq-register"/>
                   </stream:features>

}