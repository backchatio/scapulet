package io.backchat.scapulet

import ComponentConnection._
import org.specs2.specification.After
import akka.actor.Props
import akka.util.duration._
import org.jboss.netty.channel._
import akka.testkit._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.specs2.mock.Mockito

class ComponentConnectionSpec extends AkkaSpecification {
  def is = sequential ^
    "A Component connection should" ^
      "when extracting" ^
        "find an id and address in a valid stream response" ! validStreamResponse ^
        "find the error message in an error response" ! invalidStreamResponse ^ bt ^
      "when connecting" ^
        "connect to the server socket" ! specify().connectsToServer ^
        "open a stream by sending the header" ! specify().opensStreamWithHeader ^
        "reply to stream header with handshake" ! specify("challenge").sendsHandshake ^ bt ^
      "when connected and handling incoming stanzas" ^
        "send them to handlers" ! specify("succeed").sendsStanzasToHandlers ^
        "not send to handlers with failed predicates" ! specify("succeed").skipsFailedPredicates ^
    end

  def validStreamResponse = {
    val streamResp = "<?xml version='1.0'?><stream:stream xmlns:stream='http://etherx.jabber.org/streams' xmlns='jabber:component:accept' id='964412389' from='projects.backchat.im.local'>"
    StreamResponse.unapply(streamResp) must beSome(("964412389", "projects.backchat.im.local"))
  }

  def invalidStreamResponse = {
    val errorResp = "<stream:error><not-authorized xmlns='urn:ietf:params:xml:ns:xmpp-streams'/><text xmlns='urn:ietf:params:xml:ns:xmpp-streams' xml:lang='en'>Invalid Handshake</text></stream:error></stream:stream>"
    AuthenticationFailureResponse.unapply(errorResp) must beSome("Invalid Handshake")
  }

  def specify(authMode: String = "pass") = new XmppServerContext(authMode)

  class XmppServerContext(authMode: String) extends After with Mockito {
    
    implicit val executor = system.dispatcher
    val probe = TestProbe()
    implicit val self = probe.ref

    val allStanzas = TestProbe()
    val handleNone = TestProbe()

    val connConfig =
      ComponentConfig("test", "test for connection", ConnectionConfig(
        userName = "componentid",
        password = "componentpassword",
        host = "127.0.0.1",
        port = FreePort(),
        virtualHost = Some("localhost")))

    val openStream = OpenStream("componentid.localhost")
    val challenge = "<handshake>%s</handshake>".format("challengeidcomponentpassword".sha1Hex)
    var client: Option[Channel] =  None
    def pipeline = Channels.pipeline(new SimpleChannelUpstreamHandler {

      override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
        if (authMode != "succeed") client = Some(e.getChannel)
        else client = None
      }

      override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
        val msg = new String(e.getMessage.asInstanceOf[ChannelBuffer].array(), Utf8)
        authMode match {
          case "challenge" =>
            msg match {
              case `openStream` => {
                ctx.getChannel.write(ChannelBuffers.copiedBuffer(msg.replace(">", " id='challengeid'>"), Utf8))
              }
              case _ => probe.ref ! msg
            }
          case "pass" => probe.ref ! msg
          case "succeed" => {
            msg match {
              case `openStream` => {
                ctx.getChannel.write(ChannelBuffers.copiedBuffer(msg.replace(">", " id='challengeid'>"), Utf8))
              }
              case `challenge` => {
                ctx.getChannel.write(ChannelBuffers.copiedBuffer("<handshake/>", Utf8)).addListener(new ChannelFutureListener {
                  def operationComplete(future: ChannelFuture) {
                    client = Some(future.getChannel)
                  }
                })
              }
              case x => logger.error("Received unhandled: {}", x)
            }
          }
        }
      }
    })
    val server = new NettyServer(connConfig.connection, pipeline)
    server.connect()
    

    val conn = system.actorOf(Props(new ComponentConnection(Some(connConfig))), "component")
    conn ! Handler(Stanza.matching.AllStanzas, _ => false, allStanzas.ref)
    conn ! Handler(Stanza.matching("none", { case _ => false }), _ => false, handleNone.ref)
    
    def after = {
      system stop conn
      client filter (_.isConnected) foreach (_.close())
      server.close()

    }
    
    def connectsToServer = this {
      (client must not(beNone).eventually)
    }

    def opensStreamWithHeader = this {
      awaitCond(client.isDefined)
      probe.receiveOne(3 seconds) must_== openStream
    }

    def sendsHandshake = this {
      awaitCond(client.isDefined)
      probe.receiveOne(3 seconds) must_== challenge
    }

    def sendsStanzasToHandlers = this {
      awaitCond(client.isDefined)
      val stanza = <testnode>blahblah</testnode>
      client foreach { server.write(stanza, _) }
      allStanzas.receiveOne(3 seconds) must_== stanza
    }

    def skipsFailedPredicates = this {
      awaitCond(client.isDefined)
      val stanza = <testnode>blahblah</testnode>
      client foreach { server.write(stanza, _) }
      (allStanzas.receiveOne(3 seconds) must_== stanza) and {
        handleNone.expectNoMsg() must not(throwA[AssertionError])
      }
    }
  }

}

// vim: set si ts=2 sw=2 sts=2 et: