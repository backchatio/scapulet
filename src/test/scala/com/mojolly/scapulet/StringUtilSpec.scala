package com.mojolly.scapulet

/**
 * Created by IntelliJ IDEA.
 * User: ivan
 * Date: Aug 17, 2010
 * Time: 1:32:53 PM
 * 
 
 */

import com.mojolly.scapulet._
import Scapulet._


import org.specs._
import org.specs.mock.Mockito
import runner.{ScalaTest, JUnit}

object StringUtilSpec extends Specification with Mockito with JUnit with ScalaTest {
  detailedDiffs

  "The StringUtil" should {
    "must hash a given string as hex" in {
      StringUtil.hash("the string") must be_==("e8cc75db52457e014d354b54a2c44c30dd96cbd5")
    }

    "must get the resource and barejid from a jid" in {
      "the resource must be Some(value) when there is a resource" in {
        "someone@somewhere.com/theclient" match {
          case JID(bareJid, resource) => {
            bareJid must_== "someone@somewhere.com"
            resource must notBeEmpty
            resource must_== Some("theclient")
          }
          case _ => fail("Didn't parse the JID")

        }
      }

      "the resource must be None when there is no resource" in {
        "someone@somewhere.com" match {
          case JID(bareJid, resource) => {
            bareJid must_== "someone@somewhere.com"
            resource must beEmpty
          }
          case _ => fail("Didn't parse the JID")

        }
      }
    }
  }
  
}

// vim: set si ts=2 sw=2 sts=2 et: