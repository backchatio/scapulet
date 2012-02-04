package io.backchat.scapulet
package stanza

import xml._
import org.specs2.Specification

class DiscoInfoQuerySpec extends Specification {
  def is =
    "A DiscoInfoQuery should" ^
      "extract the from and id fields from disco info queries" ! {
        val stanza = "<iq type='get' from='romeo@montague.net/orchard' to='plays.shakespeare.lit' id='info1'><query xmlns='http://jabber.org/protocol/disco#info'/></iq>"
        DiscoInfoQuery.unapply(XML.loadString(stanza)) must beSome(("romeo@montague.net/orchard", "plays.shakespeare.lit", "info1", None))
      } ^
      "extract the from, id and node fields from disco info queries" ! {
        val stanza = "<iq type='get' from='romeo@montague.net/orchard' to='plays.shakespeare.lit' id='info1'><query xmlns='http://jabber.org/protocol/disco#info' node='balcony'/></iq>"
        DiscoInfoQuery.unapply(XML.loadString(stanza)) must beSome(("romeo@montague.net/orchard", "plays.shakespeare.lit", "info1", Some("balcony")))
      } ^
      "when generating a stanza" ^
      "throw an exception when no id is given" ! {
        DiscoInfoQuery(" ", "someone", "somebody") must throwA[Exception]
      } ^
      "throw an exception when no sender is defined" ! {
        DiscoInfoQuery("1234", null, "somebody") must throwA[Exception]
      } ^
      "throw an exception when no recipient is defined" ! {
        DiscoInfoQuery("2345", "someone", " ") must throwA[Exception]
      } ^
      "return a xml stanza when the request is valid" ! {
        DiscoInfoQuery("3456", "someone", "somebody") must ==/(
          (<iq type="get" id="3456" to="somebody" from="someone">
             <query xmlns="http://jabber.org/protocol/disco#info"/>
           </iq>)
        )
      } ^
      end

}