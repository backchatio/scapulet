package com.mojolly.scapulet

import com.mojolly.scapulet._
import ComponentConnection._
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers


class ComponentConnectionSpec extends WordSpec with MustMatchers {

  "A Component connection" when {

    "extracting" should {
      val streamResp =  "<?xml version='1.0'?><stream:stream xmlns:stream='http://etherx.jabber.org/streams' xmlns='jabber:component:accept' id='964412389' from='projects.backchat.im.local'>"
      val errorResp =  "<stream:error><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/><text xmlns='urn:ietf:params:xml:ns:xmpp-streams' xml:lang='en'>Invalid Handshake</text></stream:error></stream:stream>"

      "find an id and address in a valid stream response" in {
        streamResp match {
          case StreamResponse(id, from) => {
            id must equal("964412389")
            from must equal("projects.backchat.im.local")
          }
          case _ => fail("Should parse the id and from")
        }
      }

      "find the error message in an error response" in {
        errorResp match {
          case AuthenticationFailureResponse(error) => {
            error must equal("Invalid Handshake")
          }
          case _ => fail("Should have parsed the error message out of the stream.")
        }
      }
    }


  }

}

// vim: set si ts=2 sw=2 sts=2 et: