package io.backchat.scapulet

import ComponentConnection._
import org.specs2.Specification

class ComponentConnectionSpec extends Specification {
  def is =
    "A Component connection, when extracting, should" ^
      "find an id and address in a valid stream response" ! validStreamResponse ^
      "find the error message in an error response" ! invalidStreamResponse ^
      end

  def validStreamResponse = {
    val streamResp = "<?xml version='1.0'?><stream:stream xmlns:stream='http://etherx.jabber.org/streams' xmlns='jabber:component:accept' id='964412389' from='projects.backchat.im.local'>"
    StreamResponse.unapply(streamResp) must beSome(("964412389", "projects.backchat.im.local"))
  }

  def invalidStreamResponse = {
    val errorResp = "<stream:error><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/><text xmlns='urn:ietf:params:xml:ns:xmpp-streams' xml:lang='en'>Invalid Handshake</text></stream:error></stream:stream>"
    AuthenticationFailureResponse.unapply(errorResp) must beSome("Invalid Handshake")
  }

}

// vim: set si ts=2 sw=2 sts=2 et: