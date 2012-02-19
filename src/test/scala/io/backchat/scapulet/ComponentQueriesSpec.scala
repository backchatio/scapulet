package io.backchat.scapulet

import org.specs2.specification.After
import akka.actor._
import stanza.{Identity, Feature}
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import xml._

object ComponentQueriesSpec {
  class DummyHandler(implicit system: ActorSystem) extends StanzaHandler("queries-test") {
    def features: Seq[Feature] = Vector(Feature.discoInfo, Feature.discoItems)

    def identities: Seq[Identity] = Vector(Identity.component.generic)

    def handleStanza = {
      case _ => 
    }

    protected val me = ""
  }
  class DummyHandler2(implicit system: ActorSystem) extends StanzaHandler("queries-test-2") {
    def features: Seq[Feature] = Vector(Feature.xmppPing, Feature.xmppReceipts)

    def identities: Seq[Identity] = Vector(Identity.collaboration.whiteboard)


    protected val me = ""

    def handleStanza = {
      case _ =>
    }
  }
}

class ComponentQueriesSpec extends AkkaSpecification { def is = sequential ^
  "When querying a component, it should" ^
    "respond to feature queries" ! specify.respondsToFeatureQueries ^
    "respond to identities queries" ! specify.respondsToIdenityQueries ^
    "respond to component info queries" ! specify.respondsToComponentInfoQueries ^
  end

  def specify = new ComponentQueriesContext
  class ComponentQueriesContext extends After {
    
    import ComponentQueriesSpec._
    import StanzaHandler.Messages._
    implicit val executor = system.dispatcher
    implicit val timeout = Timeout(2 seconds)
    
    val connConfig =
          ComponentConfig("component", "test", "test for connection", ConnectionConfig(
            userName = "componentid",
            password = "componentpassword",
            host = "127.0.0.1",
            port = FreePort(),
            virtualHost = Some("localhost")))
    val conn = system.actorOf(Props(new ComponentConnection(Some(connConfig))), connConfig.id)
    val hand = new DummyHandler
    val hand2 = new DummyHandler2
    conn ! Register(hand)
    conn ! Register(hand2)

    def after = {
      system stop conn
    }
    
    def respondsToFeatureQueries = this {
      Await.result((conn ? Features).mapTo[Seq[Feature]], 2 seconds) must haveTheSameElementsAs(hand.features ++ hand2.features )
    }
    
    def respondsToIdenityQueries = this {
      Await.result((conn ? Identities).mapTo[Seq[Identity]], 2 seconds) must haveTheSameElementsAs(hand.identities ++ hand2.identities)
    }
    
    def respondsToComponentInfoQueries = this {
      val componentInfos = NodeSeq.fromSeq((hand.identities ++ hand2.identities).map(_.toXml) ++ (hand.features ++ hand2.features).map(_.toXml))
      Await.result((conn ? Infos).mapTo[NodeSeq], 2 seconds) must ==/(componentInfos)
    }
  }

}
