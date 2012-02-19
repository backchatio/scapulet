package io.backchat.scapulet
package components

import akka.actor.ActorSystem
import stanza.{ DiscoInfoQuery, Identity, Feature }

/**
 * The ability to discover information about entities on the Jabber network is extremely valuable. Such information
 * might include features offered or protocols supported by the entity, the entity's type or identity, and additional
 * entities that are associated with the original entity in some way (often thought of as "children" of the "parent" entity).
 * @param system [[akka.actor.ActorSystem]]
 */
class ServiceDiscovery(val version: String)(implicit system: ActorSystem) extends StanzaHandler("service-discovery") {
  val features: Seq[Feature] = Vector(Feature.discoInfo, Feature.discoItems)
  val identities: Seq[Identity] = Vector.empty

  def handleStanza = {
    case DiscoInfoQuery(from, to, id, None) if !to.contains("@") ⇒ safeReplyWith(from, to) {
      iqReply(ns.DiscoInfo, id, to, from) {
        ((identities map (_.toXml)) ++ (features map (_.toXml))) ++
          <x xmlns="jabber:x:data" type="result">
            <field var="FORM_TYPE" type="hidden">
              <value>http://jabber.org/network/serverinfo</value>
            </field>
          </x>
      }
    }
    case DiscoInfoQuery(from, to, id, _) ⇒ safeReplyWith(from, to) {
      iqReply(ns.DiscoInfo, id, to, from) {
        <query xmlns={ ns.DiscoInfo } node={ ns.ProtocolCommands }/>
        <error type='cancel'>
          <service-unavailable xmlns={ ns.Stanza }/>
        </error>
      }
    }

  }
}
