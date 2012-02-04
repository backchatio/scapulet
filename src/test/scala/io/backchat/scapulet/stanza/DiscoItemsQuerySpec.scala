package io.backchat.scapulet.stanza

import xml._
import org.specs2.Specification

class DiscoItemsQuerySpec extends Specification {
  def is =

    "A DiscoItemQuery should" ^
      "when extracting" ^
      "extract the from and id fields from disco item queries" ! {
        val stanza = "<iq type='get' from='romeo@montague.net/orchard' to='plays.shakespeare.lit' id='info1'><query xmlns='http://jabber.org/protocol/disco#items'/></iq>"
        DiscoItemsQuery.unapply(XML.loadString(stanza)) must beSome(("romeo@montague.net/orchard", "plays.shakespeare.lit", "info1", None))
      } ^
      "extract the from, id and node fields from disco item queries" ! {
        val stanza = "<iq type='get' from='romeo@montague.net/orchard' to='plays.shakespeare.lit' id='info1'><query xmlns='http://jabber.org/protocol/disco#items' node='balcony'/></iq>"
        DiscoItemsQuery.unapply(XML.loadString(stanza)) must beSome(("romeo@montague.net/orchard", "plays.shakespeare.lit", "info1", Some("balcony")))
      } ^
      "generating" ^
      "throw an exception when no id is given" ! {
        DiscoItemsQuery(" ", "someone", "somebody")(Nil) must throwA[Exception]
      } ^
      "throw an exception when no sender is defined" ! {
        DiscoItemsQuery("1234", null, "somebody")(Nil) must throwA[Exception]
      } ^
      "throw an exception when no recipient is defined" ! {
        DiscoItemsQuery("2345", "someone", " ")(Nil) must throwA[Exception]
      } ^
      "return a xml stanza when the request is valid" ! {
        (DiscoItemsQuery("3456", "someone", "somebody") { DiscoItem("somebody") }) must ==/(
          <iq type="get" id="3456" to="somebody" from="someone">
            <query xmlns="http://jabber.org/protocol/disco#items">
              <item jid="somebody"/>
            </query>
          </iq>)
      }
}