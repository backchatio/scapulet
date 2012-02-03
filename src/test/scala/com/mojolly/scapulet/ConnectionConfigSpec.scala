package com.mojolly.scapulet

import com.mojolly.scapulet._

import Scapulet.ConnectionConfig
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class ConnectionConfigSpec extends WordSpec with MustMatchers {

  val config = ConnectionConfig("user", "testsecret", "somewhere", 5678, Some("jabber.tld"))
  val configNoVirt = ConnectionConfig("user", "testsecret", "somewhere", 5678)

  "return a virtual host domain if one is provided" in {
    config.domain must equal("jabber.tld")
  }

  "return the normal host for domain if no virtual host is provided" in {
    configNoVirt.domain must equal("somewhere")
  }

  "return the address with the correct domain" in {
    config.address must equal("user.jabber.tld")
  }

  "return the address with the correct domain when no virtual host is provided" in {
    configNoVirt.address must equal("user.somewhere")
  }

  "encode the password with the provided id as hex" in {
    config.asHexSecret("23456") must equal("ed24d5e4f63ffac89e5b5252da245d3027213f30")
  }
}

// vim: set si ts=2 sw=2 sts=2 et: