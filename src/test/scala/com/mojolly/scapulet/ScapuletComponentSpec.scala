package com.mojolly.scapulet

import com.mojolly.scapulet._

import org.specs._
import org.specs.mock.Mockito
import runner.{ScalaTest, JUnit}
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.actor.ActorRef
import scala.collection.JavaConversions._
import org.multiverse.api.latches.StandardLatch
import java.util.concurrent.{TimeUnit, CountDownLatch, ConcurrentSkipListSet}
import com.mojolly.scapulet.Scapulet._
import com.mojolly.scapulet.ComponentConnection.FaultTolerantComponentConnection

object ScapuletComponentSpec extends Specification with Mockito with JUnit with ScalaTest {

  "The processor" should {

    val conn = smartMock[FaultTolerantComponentConnection]
    "register handlers" in {
      val handlers = new ConcurrentSkipListSet[ActorRef]
      val latch = new StandardLatch
      val callback = actor {
        case RegisteredHandler(_) => latch.open
      }
      val processor = actorOf(ScapuletComponent(conn, handlers, Some(callback))).start
      val handler = actor { case "a message" =>  }
      processor ! RegisterHandler(handler)
      latch.await
      handlers.isEmpty must be(false)
      handlers.contains(handler) must be(true)
    }

    "unregister handlers" in {
      val handlers = new ConcurrentSkipListSet[ActorRef]
      val latch = new CountDownLatch(2)
      val registerLatch = new StandardLatch
      val callback = actor {
        case RegisteredHandler(_) => {
          latch.countDown
          registerLatch.open
        }
        case UnregisteredHandler(_) => latch.countDown
      }
      val processor = actorOf(ScapuletComponent(conn, handlers, Some(callback))).start
      val handler = actor { case "a message" => }
      processor ! RegisterHandler(handler)
      registerLatch.await
      processor ! UnregisterHandler(handler)
      latch.await
      handlers.contains(handler) must be(false)
    }

    "respond to messages defined in the handlers" in {
      val latch = new CountDownLatch(1)
      val handler = actor { case "a message" => latch.countDown }
      val handlers = new ConcurrentSkipListSet[ActorRef]((handler :: Nil).toList)
      val processor = actorOf(ScapuletComponent(conn, handlers)).start
      processor ! "a message"
      latch.await(2, TimeUnit.SECONDS)
      latch.getCount must_== 0
    }
    
    "indicate it can respond to messages defined in the handlers" in {
      val handler = actor {
        case "a message" =>
      }
      val handlers = new ConcurrentSkipListSet[ActorRef]((handler :: Nil).toList)
      val processor = actorOf(ScapuletComponent(conn, handlers)).start
      processor.isDefinedAt("a message") must be(true)
    }

    "indicate it can't respond to messages defined in the handlers" in {
      val handler = actor {
        case "a message" =>
      }
      val handlers = new ConcurrentSkipListSet[ActorRef]((handler :: Nil).toList)
      val processor = actorOf(ScapuletComponent(conn, handlers)).start
      processor.isDefinedAt("the message") must be(false)
    }
  }

}

// vim: set si ts=2 sw=2 sts=2 et: