package com.mojolly.scapulet

import com.mojolly.scapulet._
import Scapulet._
import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec


class StringUtilSpec extends WordSpec with MustMatchers {

  "The StringUtil" when {
    "must hash a given string as hex" in {
      StringUtil.hash("the string") must equal("e8cc75db52457e014d354b54a2c44c30dd96cbd5")
    }

    "must get the resource and barejid from a jid" should {
      "the resource must be Some(value) when there is a resource" in {
        "someone@somewhere.com/theclient" match {
          case JID(bareJid, resource) => {
            bareJid must equal( "someone@somewhere.com")
            resource must not be ('empty)
            resource must equal( Some("theclient"))
          }
          case _ => fail("Didn't parse the JID")

        }
      }

      "the resource must be None when there is no resource" in {
        "someone@somewhere.com" match {
          case JID(bareJid, resource) => {
            bareJid must equal( "someone@somewhere.com")
            resource must be ('empty)
          }
          case _ => fail("Didn't parse the JID")

        }
      }
    }
  }
  
}

// vim: set si ts=2 sw=2 sts=2 et: