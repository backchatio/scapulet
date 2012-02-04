package io.backchat.scapulet
package stanza

import scala.xml._

object StreamFeatures {

  def apply(requireTLS: Boolean = true,
    SASLMechanisms: Seq[String] = List("PLAIN", "DIGEST-MD5"),
    compressionMethods: Seq[String] = Nil,
    extraFeatures: Seq[Node] = Seq.empty) = {
    <stream:features>
      <starttls xmlns={ ns.Tls }/>
      { addCompressionMethods(compressionMethods) }
      <mechanisms xmlns={ ns.Sasl }>
        { SASLMechanisms.map(m => <mechanism>{ m }</mechanism>) }
      </mechanisms>
      { extraFeatures }
    </stream:features>.map(Utility.trim(_)).theSeq.head
  }

  private def addCompressionMethods(methods: Seq[String]): NodeSeq = {
    if (!methods.isEmpty) {
      <compression xmlns={ ns.Compression }>
        { methods.map(m => <method>{ m }</method>) }
      </compression>
    } else {
      Nil
    }
  }

  def unapply(stanza: Node) = stanza.map(Utility.trim(_)).theSeq.head match {
    case feat @ <stream:features>{ ch @ _* }</stream:features> => {
      val tls = !(feat \ "starttls").isEmpty
      val mechanisms = (feat \\ "mechanism").map(_.text)
      val compressionMethods = (feat \\ "methods").map(_.text)
      val others = ch.filterNot(n => ("starttls" :: "mechanisms" :: Nil).contains(n.label))

      Some((tls, mechanisms, NodeSeq.fromSeq(others)))
    }
    case _ => None
  }
}
