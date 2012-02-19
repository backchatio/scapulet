package io.backchat.scapulet

import org.specs2.specification.After
import akka.util.Timeout
import io.backchat.scapulet.ComponentConnection.IncomingStanza
import akka.testkit._
import scala.xml._
import akka.util.duration._
import stanza.{Feature, DiscoInfoQuery}

object ServiceDiscoverySpec {
  val conf = """
scapulet {
  components {
    testcomponent {
    }
  }
}
"""
}
class ServiceDiscoverySpec extends AkkaSpecification(ServiceDiscoverySpec.conf) {

  def is = sequential ^
    "When providing service discovery the component should" ^
      "reply to a top level info query" ^
        "with a info response" ! specify.repliesToInfoRequest ^
        "with error if not a component request" ! specify.repliesWithErrorForInvalid ^
    end
  
  def specify = new ServiceDiscoveryContext
  
  class ServiceDiscoveryContext extends After {

    import ComponentQueriesSpec.DummyHandler2
    import StanzaHandler.Messages.Register
    implicit val executor = system.dispatcher
    implicit val timeout = Timeout(2 seconds)
    val probe = TestProbe()
    
    val connConfig =
          ComponentConfig("testcomponent", "test", "test for connection", ConnectionConfig(
            userName = "componentid",
            password = "componentpassword",
            host = "127.0.0.1",
            port = FreePort(),
            virtualHost = Some("localhost")))
    
    val conn = system.scapulet.componentConnection(connConfig.id, Some(connConfig), Some(probe.ref))
    probe.expectMsg(Scapulet.Connect)
    val hand = new DummyHandler2
    conn ! Register(hand)
    probe.ignoreMsg({
      case _: NodeSeq => false
      case _ => true
    })

    def after = {
      system stop conn
      system stop probe.ref
    }
    
    def repliesToInfoRequest = this {
      conn ! IncomingStanza(DiscoInfoQuery("bla134", "tom@capulet.com/blah", connConfig.connection.address))
      val msg = probe.receiveOne(2 seconds).asInstanceOf[NodeSeq]
      msg \\ "identity" must haveTheSameElementsAs(hand.identities.map(_.toXml)) and {
        msg \\ "feature" must haveTheSameElementsAs((Seq(Feature.discoInfo, Feature.discoItems) ++ hand.features).map(_.toXml))
      }
    }

    def repliesWithErrorForInvalid = this {
      conn ! IncomingStanza(DiscoInfoQuery("bla134", "tom@capulet.com/blah", "wrong@" + connConfig.connection.address))
      val msg = probe.receiveOne(2 seconds).asInstanceOf[NodeSeq]
      msg \\ "identity" must beEmpty and {
        msg \\ "feature" must beEmpty 
      } and {
        (msg \\ "error").head must ==/(<error type="cancel">
                  <service-unavailable xmlns={ ns.Stanza }/>
                </error>)
      }
    }
  }
}
