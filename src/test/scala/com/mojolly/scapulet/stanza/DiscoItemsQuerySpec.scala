package com.mojolly.scapulet.stanza

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import xml._

class DiscoItemsQuerySpec extends WordSpec with MustMatchers {

  "A DiscoItemQuery" when {

    "extracting" should {
      "extract the from and id fields from disco item queries" in {
        val stanza = "<iq type='get' from='romeo@montague.net/orchard' to='plays.shakespeare.lit' id='info1'><query xmlns='http://jabber.org/protocol/disco#items'/></iq>"
        XML.loadString(stanza) match {
          case DiscoItemsQuery(from, to, id, node) => {
            to must be ("plays.shakespeare.lit")
            from must be ("romeo@montague.net/orchard")
            id must be ("info1")
            node must be ('empty)
          }
          case _ => fail("The extractor didn't find the address")
        }
      }

      "extract the from, id and node fields from disco item queries" in {
        val stanza = "<iq type='get' from='romeo@montague.net/orchard' to='plays.shakespeare.lit' id='info1'><query xmlns='http://jabber.org/protocol/disco#items' node='balcony'/></iq>"
        XML.loadString(stanza) match {
          case DiscoItemsQuery(from, to, id, node) => {
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
        evaluating { DiscoItemsQuery(" ", "someone", "somebody")(Nil) } must produce [Exception]
      }
      "throw an exception when no sender is defined" in {
        evaluating { DiscoItemsQuery("1234", null, "somebody")(Nil) } must produce [Exception]
      }
      "throw an exception when no recipient is defined" in {
        evaluating { DiscoItemsQuery("2345", "someone", " ")(Nil) } must produce [Exception]
      }
      "return a xml stanza when the request is valid" in {
        (DiscoItemsQuery("3456", "someone", "somebody") { DiscoItem("somebody") }).map(Utility.trimProper _) must equal (
          (<iq type="get" id="3456" to="somebody" from="someone">
            <query xmlns="http://jabber.org/protocol/disco#items">
              <item jid="somebody" />
            </query>
          </iq>).map(Utility.trimProper _)
        )
      }


    }
    
  }
}