package com.mojolly.scapulet.stanza

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import xml.Utility
import io.backchat.scapulet.stanza.DiscoItem

class DiscoItemSpec extends WordSpec with MustMatchers {

  "A DiscoItem" when {

    "extracting" should {

      "get the jid" in {
        val st = <item jid="somebody"/>
        st match {
          case DiscoItem(jid, _, _) => jid must be("somebody")
          case _ => fail("Couldn't get the jid out of the disco item")
        }
      }
      "get the node if one is provided" in {
        val st = <item jid="somebody" node="theNode"/>
        st match {
          case DiscoItem(_, _, Some(node)) => node must be("theNode")
          case _ => fail("the node name did not match")
        }
      }
      "return none for the node if none is provided" in {
        val st = <item jid="somebody"/>
        st match {
          case DiscoItem(_, _, node) => node must be('empty)
          case _ => fail("The node was not none or we couldn't parse the stanza")
        }
      }
      "get the name if one is provided" in {
        val st = <item jid="somebody" name="theName"/>
        st match {
          case DiscoItem(_, Some(name), _) => name must be("theName")
          case _ => fail("The name was not none or we couldn't parse the stanza")
        }
      }
      "return none for the name if none is provided" in {
        val st = <item jid="somebody"/>
        st match {
          case DiscoItem(_, name, _) => name must be('empty)
          case _ => fail("The name was not none or we couldn't parse the stanza")
        }
      }
      "not match when there is no jid provided" in {
        val st = <item node="somebody" name="theName"/>
        DiscoItem.unapply(st) must be('empty)
      }
      "not match when it is a different element" in {
        val st = <item2 jid="somebody" node="theNode" name="theName"/>
        DiscoItem.unapply(st) must be('empty)
      }
    }

    "generating" should {
      "throw an exception when no jid is provided" in {
        evaluating { DiscoItem("  ") } must produce[Exception]
      }
      "generate xml with all attributes when all are provided" in {
        val it = DiscoItem("somebody", Some("theName"), Some("theNode"))
        (it \ "@name").text must be("theName")
        (it \ "@node").text must be("theNode")
        (it \ "@jid").text must be("somebody")
      }
      "generate xml without a node attribute when none is provided" in {
        DiscoItem("somebody", Some("theName")).map(Utility.trimProper _) must be(
          (<item jid="somebody" name="theName"/>).map(Utility.trimProper _)
        )
      }
      "generate xml without a name attribute when none is provided" in {
        DiscoItem("somebody", node = Some("theNode")).map(Utility.trimProper _) must be(
          (<item jid="somebody" node="theNode"/>).map(Utility.trimProper _)
        )
      }
    }
  }
}