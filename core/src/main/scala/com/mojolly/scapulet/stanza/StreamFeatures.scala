package com.mojolly.scapulet
package stanza

import xml._
import XMPPConstants._
import util.Implicits._



object StreamFeatures {

  def apply( requireTLS: Boolean = true,
             SASLMechanisms: Seq[String] = List("PLAIN", "DIGEST-MD5"),
             extraFeatures: Seq[Node]= Seq.empty ) = {
    <stream:features>
      <starttls xmlns={TLS_NS} />
      <mechanisms xmlns={SASL_NS}>
        {SASLMechanisms.map(m => <mechanism>{m}</mechanism>)}
      </mechanisms>
      {extraFeatures}
    </stream:features>.map(Utility.trim(_)).theSeq.head
  }

  def unapply(stanza: Node) = stanza.map(Utility.trim(_)).theSeq.head match {
    case feat @ <stream:features>{ ch @ _* }</stream:features> => {
      val tls = !(feat \ "starttls").isEmpty
      val mechanisms = (feat \\ "mechanism").map(_.text)
      val others = ch.filterNot(n => ("starttls" :: "mechanisms" :: Nil).contains(n.label))
      Some((tls, mechanisms, NodeSeq.fromSeq(others)))
    }
    case _ => None
  }
}
