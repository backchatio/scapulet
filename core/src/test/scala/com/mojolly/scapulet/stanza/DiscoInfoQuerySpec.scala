package com.mojolly.scapulet.stanza

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import xml.XML

class DiscoInfoQuerySpec extends WordSpec with MustMatchers {

  "DiscoInfoQuery extractor" should {

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
}