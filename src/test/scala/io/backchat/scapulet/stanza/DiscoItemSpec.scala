package io.backchat.scapulet
package stanza

import org.specs2.Specification
import xml.{ Elem, NodeSeq }

class DiscoItemSpec extends Specification {
  def is =
    "A DiscoItem should" ^
      "when extracting" ^
      "get the jid" ! { DiscoItem.unapply(<item jid="somebody"/>) must beSome(("somebody", None, None)) } ^
      "get the node if one is provided" ! {
        DiscoItem.unapply(<item jid="somebody" node="theNode"/>) must beSome(("somebody", None, Some("theNode")))
      } ^
      "get the name if one is provided" ! {
        DiscoItem.unapply(<item jid="somebody" name="theName"/>) must beSome(("somebody", Some("theName"), None))
      } ^
      "not match when there is no jid provided" ! {
        DiscoItem.unapply(<item node="somebody" name="theName"/>) must beNone
      } ^
      "not match when it is a different element" ! {
        DiscoItem.unapply(<item2 jid="somebody" node="theNode" name="theName"/>) must beNone
      } ^ bt ^
      "when generating" ^
      "throw an exception when no jid is provided" ! { DiscoItem("  ") must throwA[Exception] } ^
      "generate xml with all attributes when all are provided" ! {
        DiscoItem("somebody", Some("theName"), Some("theNode")) must \\("item", "name" -> "theName", "node" -> "theNode", "jid" -> "somebody")
      } ^
      "generate xml without a node attribute when none is provided" ! {
        DiscoItem("somebody", Some("theName")) must ==/(<item jid="somebody" name="theName"/>)
      } ^
      "generate xml without a name attribute when none is provided" ! {
        DiscoItem("somebody", node = Some("theNode")) must ==/(<item jid="somebody" node="theNode"/>)
      } ^
      end

}