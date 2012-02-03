package com.mojolly.scapulet.stanza

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import xml._
import io.backchat.scapulet.stanza.DiscoInfoQuery

class DiscoInfoQuerySpec extends WordSpec with MustMatchers {

  "A DiscoInfoQuery" when {

    "extracting" should {

      "extract the from and id fields from disco info queries" in {
        val stanza = "<iq type='get' from='romeo@montague.net/orchard' to='plays.shakespeare.lit' id='info1'><query xmlns='http://jabber.org/protocol/disco#info'/></iq>"
        XML.loadString(stanza) match {
          case DiscoInfoQuery(from, to, id, node) => {
            to must be ("plays.shakespeare.lit")
            from must be ("romeo@montague.net/orchard")
            id must be ("info1")
            node must be ('empty)
          }
          case _ => fail("The extractor didn't find the address")
        }
      }

      "extract the from, id and node fields from disco info queries" in {
        val stanza = "<iq type='get' from='romeo@montague.net/orchard' to='plays.shakespeare.lit' id='info1'><query xmlns='http://jabber.org/protocol/disco#info' node='balcony'/></iq>"
        XML.loadString(stanza) match {
          case DiscoInfoQuery(from, to, id, node) => {
            to must be ("plays.shakespeare.lit")
            from must be ("romeo@montague.net/orchard")
            id must be ("info1")
            node must be (Some("balcony"))
          }
          case _ => fail("The extractor didn't find the address")
        }
      }
    }

    "generating" should {

      "throw an exception when no id is given" in {
        evaluating { DiscoInfoQuery(" ", "someone", "somebody") } must produce [Exception]
      }
      "throw an exception when no sender is defined" in {
        evaluating { DiscoInfoQuery("1234", null, "somebody") } must produce [Exception]
      }
      "throw an exception when no recipient is defined" in {
        evaluating { DiscoInfoQuery("2345", "someone", " ") } must produce [Exception]
      }
      "return a xml stanza when the request is valid" in {
        DiscoInfoQuery("3456", "someone", "somebody").map(Utility.trimProper _) must equal (
          (<iq type="get" id="3456" to="somebody" from="someone">
            <query xmlns="http://jabber.org/protocol/disco#info" />
          </iq>).map(Utility.trimProper _)
        )
      }
    }

  }
}