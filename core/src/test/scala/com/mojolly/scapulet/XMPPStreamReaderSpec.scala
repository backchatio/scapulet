package com.mojolly.scapulet

import com.mojolly.scapulet._

import org.specs._
import org.specs.mock.Mockito
import runner.{ScalaTest, JUnit}
import scala.io.Source
import scala.xml._
import pull._
import se.scalablesolutions.akka.util.Logging
import java.io.ByteArrayInputStream

object XMPPStreamReaderSpec extends Specification with Mockito with JUnit with ScalaTest {
  detailedDiffs

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
          <p>The <acronym>XHTML</acronym> user agent conformance
             requirements say to&#160;ignore elements and attributes
             you don't understand, to wit:</p>
          <ol type='1' start='4'>
            <li><p>
              If a user agent encounters an element it does
              not recognize, it must continue to process the
              children of that element. If the content is text,
              the text must be presented to the user.
            </p></li>
            <li><p>
              If a user agent encounters an attribute it does
              not recognize, it must ignore the entire attribute
              specification (i.e., the attribute &amp; its value).
            </p></li>
          </ol>
        </body>
      </html>
    </message>


  "The XMPPStreamReader" should {
    "Parse a message object" in {
      val rdr = new XMPPStreamReader(new ByteArrayInputStream(sample.toString.getBytes("UTF-8")))
      val res = rdr.read

      (res \ "body").text must_== (sample \ "body").text
      (res \ "html" \ "body").text must_== (sample \ "html" \ "body").text
      (res \ "html" \ "body") must \("ol", "type" -> "1", "start" -> "4")
    }
  }

}

// vim: set si ts=2 sw=2 sts=2 et: