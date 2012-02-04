package io.backchat.scapulet
package stanza

import xml._
import StreamErrors._
import org.specs2.Specification

class BadFormatSpec extends Specification {
  def is =
    "A BadFormat error should" ^
      "match an error without a text and app condition" ! {
        BadFormat.unapply(errorStanza) must beSome((None, NodeSeq.Empty))
      } ^
      "match an error with a text but without an app condition" ! {
        val em = "The error message"
        BadFormat.unapply(errorStanzaWithText(em)) must beSome((Some(em), NodeSeq.Empty))
      } ^
      "match an error without a text but with an app condition" ! {
        val aa = NodeSeq.fromSeq(Seq(<the-condition/>))
        BadFormat.unapply(errorStanzaWithAppCondition(aa)) must beSome((None, aa))
      } ^
      "match an error with a text and app condition" ! {
        val em = "The error message"
        val ac = NodeSeq.fromSeq(Seq(<the-condition/>))
        BadFormat.unapply(errorStanzaWithTextAndAppCondition(em, ac)) must beSome((Some(em), ac))
      } ^
      end

  def errorStanzaWithText(text: String): Node = <stream:error>
                                                  <bad-format xmlns={ ns.XmppStream }/>
                                                  <text xmlns={ ns.XmppStream }>{ text }</text>
                                                </stream:error>

  def errorStanzaWithTextAndAppCondition(text: String, appCond: Seq[Node]): Node = <stream:error>
                                                                                     <bad-format xmlns={ ns.XmppStream }/>
                                                                                     <text xmlns={ ns.XmppStream }>{ text }</text>
                                                                                     { appCond }
                                                                                   </stream:error>

  def errorStanzaWithAppCondition(appCond: Seq[Node]): Node = <stream:error>
                                                                <bad-format xmlns={ ns.XmppStream }/>
                                                                { appCond }
                                                              </stream:error>

  protected val errorStanza: Node = <stream:error>
                                      <bad-format xmlns={ ns.XmppStream }/>
                                    </stream:error>

}