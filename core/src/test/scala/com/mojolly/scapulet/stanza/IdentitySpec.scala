package com.mojolly.scapulet.stanza

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class IdentitySpec extends WordSpec with MustMatchers {

  "An Identity stanza" when {

    "extracting a valid stanza" should {

      "get the category name" in {
        val stanza = <identity category="pubsub" type="service" />
        stanza match {
          case Identity(category, _, _) => category must be ("pubsub")
          case _ => fail("Could not get the category name")
        }
      }

      "get the type name" in {
        val stanza = <identity category="pubsub" type="service" />
        stanza match {
          case Identity(_, svcType, _) => svcType must be ("service")
          case _ => fail("could not get the type")
        }
      }

      "get the name if one is given" in {
        val stanza = <identity category="pubsub" type="service" name="theName" />
        stanza match {
          case Identity(_, _, Some(nm)) => nm must be ("theName")
          case _ => fail("could not get the name")
        }
      }

      "return none for the name if none is given" in {
        val stanza = <identity category="pubsub" type="service" />
        stanza match {
          case Identity(_, _, nm) => nm must be ('empty)
          case _ => fail("could not get the name")
        }
      }
    }

    "extracting an invalid stanza" should {
      "not match when no category is given" in {
        val stanza = <identity type="service" name="theName" />
        Identity.unapply(stanza) must be ('empty)
      }
      "not match when no type is given" in {
        val stanza = <identity category="pubsub" name="theName" />
        Identity.unapply(stanza) must be ('empty)
      }
      "not match for a different element" in {
        val stanza = <identity2 category="pubsub" type="service" name="theName" />
        Identity.unapply(stanza) must be ('empty)
      }
    }

    "generating" should {

      "throw an exception when the category is null" in {
        evaluating { Identity(null, "service") } must produce [Exception]
      }

      "throw an exception when the category is blank" in {
        evaluating { Identity("  ", "service") } must produce [Exception]
      }

      "throw an exception when the type is null" in {
        evaluating { Identity("pubsub", null) } must produce [Exception]
      }

      "throw an exception when the type is blank" in {
        evaluating { Identity("pubsub", "  ") } must produce [Exception]
      }

      "generate a stanza without a name when none is provided" in {
        Identity("pubsub", "service") must be (<identity category="pubsub" type="service" />)
      }

      "generate a stanza with a name when one is provided" in {
        Identity("pubsub", "service", Some("blah")) must be (<identity category="pubsub" type="service" name="blah" />)
      }
    }
  }
}