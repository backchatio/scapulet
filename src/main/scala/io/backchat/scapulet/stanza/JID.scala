package io.backchat.scapulet
package stanza

object JID {
  def apply(bareJid: String, resource: String) = "%s/%s".format(bareJid, resource)

  def unapply(jid: String) = if (jid.contains("/")) {
    val parts = jid.split("/")
    Some((parts.head, parts.lastOption))
  } else Some((jid, None))
}

