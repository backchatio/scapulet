package io.backchat.scapulet

import akka.testkit._
import akka.util.duration._
import org.specs2.specification.After

class ComponentListenerServerSpec extends AkkaSpecification { def is =
  
  "A ComponentListenerServer should" ^  
    "accept to component connection on the server socket" ! specify().acceptsConnection ^
    "open a stream by sending the header" ! specify().repliesToHeader ^
    "reply to stream header with handshake" ! specify().verifiesHandshake ^ bt ^
  end

  def specify() = new ComponentListenerContext

  
  class ComponentListenerContext extends After {
    implicit val executor = system.dispatcher
    val probe = TestProbe()
    implicit val self = probe.ref

    val connConfig =
      ComponentConfig("test", "test for connection", ConnectionConfig(
        userName = "acomponent",
        password = "componentpassword",
        host = "127.0.0.1",
        port = FreePort(),
        virtualHost = Some("localhost")))


    def after = {

    }

    def acceptsConnection = pending
    def repliesToHeader = pending
    def verifiesHandshake = pending
  }
}
