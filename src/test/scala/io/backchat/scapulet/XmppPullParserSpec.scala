package io.backchat.scapulet

import scala.xml._
import org.jboss.netty.buffer.ChannelBuffers
import org.specs2.specification.After
import akka.util.duration._

class XmppPullParserSpec extends AkkaSpecification {

  val sample = <message>
    <body>
      The XHTML user agent conformance requirements say to ignore
      elements and attributes you don't understand, to wit:

      4. If a user agent encounters an element it does
      not recognize, it must continue to process the
      children of that element. If the content is text,
      the text must be presented to the user.

      5. If a user agent encounters an attribute it does
      not recognize, it must ignore the entire attribute
      specification (i.e., the attribute and its value).
    </body>
    <html xmlns='http://jabber.org/protocol/xhtml-im'>
      <body xmlns='http://www.w3.org/1999/xhtml'>
        <p>The
          <acronym>XHTML</acronym>
          user agent conformance
          requirements say to
          &#160;
          ignore elements and attributes
          you don't understand, to wit:</p>
        <ol type='1' start='4'>
          <li>
            <p>
              If a user agent encounters an element it does
              not recognize, it must continue to process the
              children of that element. If the content is text,
              the text must be presented to the user.
            </p>
          </li>
          <li>
            <p>
              If a user agent encounters an attribute it does
              not recognize, it must ignore the entire attribute
              specification (i.e., the attribute
              &amp;
              its value).
            </p>
          </li>
        </ol>
      </body>
    </html>
  </message>

  def is = sequential ^
    "The XmppPullParser should parse" ^
      "a message object" ! specifiedBy.parsesHtmlMessage ^
      "split xml fragments" ! specifiedBy.parsesSplitFragments ^
    end

  def specifiedBy = new PullParserSpecContext
  system.scapulet.eventStream subscribe (testActor, StanzaEventBus.AllStanzas)
  
  class PullParserSpecContext extends After {
    val parser = new XmppPullParser()

    def parsesHtmlMessage = this {
      val txt = sample.toString()
      val buff = ChannelBuffers.copiedBuffer(txt.getBytes(Utf8))
      parser.parse(buff)
      
      validateSampleResult(receiveOne(3 seconds))
    }

    private def validateSampleResult(msg: AnyRef) = {
      (msg must not beNull) and
      (msg must beAnInstanceOf[Elem]) and {
        val res = msg.asInstanceOf[Elem]
        ((res \ "html" \ "body").text must_== (sample \ "html" \ "body").text) and
        ((res \ "html" \ "body" \ "ol" \ "@type").text must_== (sample \ "html" \ "body" \ "ol" \ "@type").text) and
        ((res \ "html" \ "body" \ "ol" \ "@start").text must_== (sample \ "html" \ "body" \ "ol" \ "@start").text) and
        ((res \ "html" \ "body" \ "p" \ "acronym").text must_== "XHTML")
      }
    }
    
    val frag1 = """<message>
        <body>
          The XHTML user agent conformance requirements say to ignore
          elements and attributes you don't understand, to wit:
    
          4. If a user agent encounters an element it does
          not recognize, it must continue to process the
          children of that element. If the content is text,
          the text must be presented to the user.
    
          5. If a user agent encounters an attribute it does
          not recognize, it must ignore the entire attribute
          specification (i.e., the attribute and its value).
        </body>"""
    
    val frag2 = """
        <html xmlns='http://jabber.org/protocol/xhtml-im'>
      <body xmlns='http://www.w3.org/1999/xhtml'>
        <p>The
          <acronym>XHTML</acronym>
          user agent conformance
          requirements say to
          &#160;
          ignore elements and attributes
          you don't understand, to wit:</p>
        <ol type='1' start='4'>
          <li>
            <p>
              If a user agent encounters an element it does
              not recognize, it must continue to process the
              children of that element. If the content is text,
              the text must be presented to the user.
            </p>
          </li>
          <li>
            <p>
              If a user agent encounters an attribute it does
              not recognize, it must ignore the entire attribute
              specification (i.e., the attribute
              &amp;
              its value).
            </p>
          </li>
        </ol>
      </body>
    </html>
  </message>"""
    
    def parsesSplitFragments = this {
      parser.parse(ChannelBuffers.wrappedBuffer(frag1.getBytes(Utf8)))
      receiveOne(2 seconds) must beNull and {
        parser.parse(ChannelBuffers.wrappedBuffer(frag2.getBytes(Utf8)))
        Thread.sleep(2000)
        validateSampleResult(receiveOne(2 seconds))
      }
    }
    
    def after = {
      parser.stop 
    }
  }
}

// vim: set si ts=2 sw=2 sts=2 et: