package com.mojolly.scapulet
package stanza

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import xml._
import io.backchat.scapulet.XMPPConstants
import XMPPConstants._
import io.backchat.scapulet.stanza.StreamErrors
import StreamErrors._

class BadFormatSpec extends WordSpec with MustMatchers {

  "A BadFormat error" should {

    "match an error without a text and app condition" in {
      errorStanza match {
        case BadFormat(txt, app) => {
          txt must not be ('defined)
          app must be ('empty)
        }
        case _ => fail("Couldn't match a bad-format error")
      }
    }
    "match an error with a text but without an app condition" in {
      val em = "The error message"
      errorStanzaWithText(em) match {
        case BadFormat(txt, app) => {
          txt must be ('defined)
          txt must be (Some(em))
          app must be ('empty)
        }
        case _ => fail("Couldn't match a bad-format error")
      }
    }
    "match an error without a text but with an app condition" in {
      errorStanzaWithAppCondition(<the-condition />) match {
        case BadFormat(txt, app) => {
          txt must not be ('defined)
          app must not be ('empty)
          app.toString must equal (<the-condition />.toString) // lame, I know but at least it works. Sometimes static typing bites
        }
        case _ => fail("Couldn't match a bad-format error")
      }
    }
    "match an error with a text and app condition" in {
      val em = "The error message"
      val ac = <the-condition />
      errorStanzaWithTextAndAppCondition(em, ac) match {
        case BadFormat(txt, app) => {
          txt must be ('defined)
          app must not be ('empty)
          txt must be (Some(em))
          app.toString must equal (ac.toString) //lame, I know but at least it works
        }
        case _ => fail("Couldn't match a bad-format error")
      }
    }

  }

  def errorStanzaWithText(text: String): Node = <stream:error>
        <bad-format xmlns={XMPP_STREAMS_NS} />
        <text xmlns={XMPP_STREAMS_NS}>{text}</text>
      </stream:error>

  def errorStanzaWithTextAndAppCondition(text: String, appCond: Seq[Node]): Node = <stream:error>
        <bad-format xmlns={XMPP_STREAMS_NS} />
        <text xmlns={XMPP_STREAMS_NS}>{text}</text>
        {appCond}
      </stream:error>

  def errorStanzaWithAppCondition(appCond: Seq[Node]): Node = <stream:error>
        <bad-format xmlns={XMPP_STREAMS_NS} />
        {appCond}
      </stream:error>

  protected def errorStanza: Node = <stream:error>
        <bad-format xmlns={XMPP_STREAMS_NS} />
      </stream:error>

}