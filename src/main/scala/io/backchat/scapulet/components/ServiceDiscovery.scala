package io.backchat.scapulet
package components

import akka.actor.ActorSystem
import extractors._
import xml.NodeSeq
import stanza.{ DiscoInfoQuery, Identity, Feature }

/**
 * The ability to discover information about entities on the Jabber network is extremely valuable. Such information
 * might include features offered or protocols supported by the entity, the entity's type or identity, and additional
 * entities that are associated with the original entity in some way (often thought of as "children" of the "parent" entity).
 *
 * An implementation of [[http://xmpp.org/extensions/xep-0030.html XEP-0030 Service Discovery]] for components
 *
 * @param version The version of this component
 * @param system an implicitly available actor system [[akka.actor.ActorSystem]]
 */
abstract class ServiceDiscovery(val version: String)(implicit system: ActorSystem)
    extends StanzaHandler("service-discovery") {

  val features: Seq[Feature] = Vector(Feature.discoInfo, Feature.discoItems)
  val identities: Seq[Identity] = Vector.empty

  def handleStanza = {
    case InfoQuery(iq) && NoInfoQueryNode() && ToComponent(`me`) && FromJid(from) ⇒ safeReplyWith {
      iqReply(ns.DiscoInfo)(collectInfos ++ extendedServiceDiscoveryNodes)
    }
    case InfoQuery(_) ⇒ safeReplyWith {
      iqReply(ns.DiscoInfo) {
        <query xmlns={ ns.DiscoInfo }/>
        <error type='cancel'>
          <service-unavailable xmlns={ ns.Stanza }/>
        </error>
      }
    }
  }

  protected def collectInfos: NodeSeq

  protected def extendedServiceDiscoveryNodes = NodeSeq.Empty


}

class ComponentServiceDiscovery(version: String, protected val componentConfig: ComponentConfig)(implicit system: ActorSystem)
    extends ServiceDiscovery(version) with ComponentHandler {
  import StanzaHandler.Messages._
  protected def collectInfos = infos ++ component.ask[NodeSeq](Request(Infos, Seq(actor)))
}
