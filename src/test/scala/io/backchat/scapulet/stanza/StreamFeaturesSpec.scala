package com.mojolly.scapulet
package stanza

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import xml._
import io.backchat.scapulet.XMPPConstants
import XMPPConstants._
import io.backchat.scapulet.stanza.StreamFeatures

class StreamFeaturesSpec extends WordSpec with MustMatchers {

  "The stream features extractor" should {

    "match a stanza with tls and sasl mechanisms and a register element" in {
      fullStanza match {
        case StreamFeatures(tls, sasl, extra) => {
          tls must be (true)
          sasl must be (List("DIGEST-MD5", "PLAIN"))
          extra.toString must be(<register xmlns="http://jabber.org/features/iq-register"/>.toString)
        }
        case _ => fail("Couldn't match the full stream:features stanza")
      }
    }
  }

  def fullStanza = <stream:features>
      <starttls xmlns="urn:ietf:params:xml:ns:xmpp-tls"/>
      <mechanisms xmlns="urn:ietf:params:xml:ns:xmpp-sasl">
        <mechanism>DIGEST-MD5</mechanism>
        <mechanism>PLAIN</mechanism>
      </mechanisms>
      <register xmlns="http://jabber.org/features/iq-register"/>
    </stream:features>
  
}