package io.backchat.scapulet

import org.specs2.Specification

class ConnectionConfigSpec extends Specification {

  val config = ConnectionConfig("user", "testsecret", "somewhere", 5678, Some("jabber.tld"))
  val configNoVirt = ConnectionConfig("user", "testsecret", "somewhere", 5678)

  def is =
    "A ConnectionConfig should" ^
      "return a virtual host domain if one is provided" ! {
        config.domain must_== "jabber.tld"
      } ^
      "return the normal host for domain if no virtual host is provided" ! {
        configNoVirt.domain must_== "somewhere"
      } ^
      "return the address with the correct domain" ! {
        config.address must_== "user.jabber.tld"
      } ^
      "return the address with the correct domain when no virtual host is provided" ! {
        configNoVirt.address must_== "user.somewhere"
      } ^
      "encode the password with the provided id as hex" ! {
        config.asHexSecret("23456") must_== "ed24d5e4f63ffac89e5b5252da245d3027213f30"
      } ^
      end
}

// vim: set si ts=2 sw=2 sts=2 et: