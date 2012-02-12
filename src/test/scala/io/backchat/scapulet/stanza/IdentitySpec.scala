package io.backchat.scapulet.stanza

import org.specs2.Specification

class IdentitySpec extends Specification {
  def is =
    "An Identity stanza should" ^
      "when extracting a valid stanza" ^
      "get the category and service name" ! {
        Identity.unapply(<identity category="pubsub" type="service"/>) must beSome(("pubsub", "service", None))
      } ^
      "get the name if one is given" ! {
        Identity.unapply(<identity category="pubsub" type="service" name="theName"/>) must beSome(("pubsub", "service", Some("theName")))
      } ^ bt ^
      "when extracting an invalid stanza" ^
      "not match when no category is given" ! {
        Identity.unapply(<identity type="service" name="theName"/>) must beNone
      } ^
      "not match when no type is given" ! {
        Identity.unapply(<identity category="pubsub" name="theName"/>) must beNone
      } ^
      "not match for a different element" ! {
        Identity.unapply(<identity2 category="pubsub" type="service" name="theName"/>) must beNone
      } ^ bt ^
      "when generating" ^
      "throw an exception when the category is null" ! {
        Identity(null, "service") must throwA[Exception]
      } ^
      "throw an exception when the category is blank" ! {
        Identity("  ", "service") must throwA[Exception]
      } ^
      "throw an exception when the type is null" ! {
        Identity("pubsub", null) must throwA[Exception]
      } ^
      "throw an exception when the type is blank" ! {
        Identity("pubsub", "  ") must throwA[Exception]
      } ^
      "generate a stanza without a name when none is provided" ! {
        Identity("pubsub", "service").toXml must ==/(<identity category="pubsub" type="service"/>)
      } ^
      "generate a stanza with a name when one is provided" ! {
        Identity("pubsub", "service", Some("blah")).toXml must ==/(<identity category="pubsub" type="service" name="blah"/>)
      } ^
      end
}