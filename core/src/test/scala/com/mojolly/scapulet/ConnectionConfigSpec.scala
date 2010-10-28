package com.mojolly.scapulet

/**
 * Created by IntelliJ IDEA.
 * User: ivan
 * Date: Aug 17, 2010
 * Time: 1:41:13 PM
 * 
 
 */

import com.mojolly.scapulet._

import org.specs._
import org.specs.mock.Mockito
import runner.{ScalaTest, JUnit}
import com.mojolly.scapulet.Scapulet.ConnectionConfig

object ConnectionConfigSpec extends Specification with Mockito with JUnit with ScalaTest {
  detailedDiffs

  val config = ConnectionConfig("user", "testsecret", "somewhere", 5678, Some("jabber.tld"))
  val configNoVirt = ConnectionConfig("user", "testsecret", "somewhere", 5678)

  "return a virtual host domain if one is provided" in {
    config.domain must be_==("jabber.tld")
  }

  "return the normal host for domain if no virtual host is provided" in {
    configNoVirt.domain must be_==("somewhere")
  }

  "return the address with the correct domain" in {
    config.address must be_==("user.jabber.tld")
  }

  "return the address with the correct domain when no virtual host is provided" in {
    configNoVirt.address must be_==("user.somewhere")
  }

  "encode the password with the provided id as hex" in {
    config.asHexSecret("23456") must be_==("ed24d5e4f63ffac89e5b5252da245d3027213f30")
  }
}

// vim: set si ts=2 sw=2 sts=2 et: