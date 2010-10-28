package com.mojolly.scapulet

import com.mojolly.scapulet._

import se.scalablesolutions.akka.actor.Actor
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.actor.ActorRef
import scala.collection.JavaConversions._
import org.multiverse.api.latches.StandardLatch
import java.util.concurrent.{TimeUnit, CountDownLatch, ConcurrentSkipListSet}
import com.mojolly.scapulet.Scapulet._
import com.mojolly.scapulet.ComponentConnection.FaultTolerantComponentConnection
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar

class ScapuletComponentSpec extends WordSpec with MustMatchers with MockitoSugar {

  "The processor" should {

    val conn = mock[FaultTolerantComponentConnection]
    "register handlers" in {
      val handlers = new ConcurrentSkipListSet[ActorRef]
      val latch = new StandardLatch
      val callback = actorOf(new Actor { def receive = { case RegisteredHandler(_) => latch.open } }).start
      val processor = actorOf(new ScapuletComponent(conn, handlers, Some(callback))).start
      val handler = actorOf(new Actor { def receive = { case "a message" =>  } }).start
      processor ! RegisterHandler(handler)
      latch.await
      handlers.isEmpty must equal(false)
      handlers.contains(handler) must equal(true)
    }

    "unregister handlers" in {
      val handlers = new ConcurrentSkipListSet[ActorRef]
      val latch = new CountDownLatch(2)
      val registerLatch = new StandardLatch
      val callback = actorOf(new Actor { def receive = {
        case RegisteredHandler(_) => {
          latch.countDown
          registerLatch.open
        }
        case UnregisteredHandler(_) => latch.countDown
      } }).start
      val processor = actorOf(new ScapuletComponent(conn, handlers, Some(callback))).start
      val handler = actorOf(new Actor { def receive = { case "a message" =>  } }).start
      processor ! RegisterHandler(handler)
      registerLatch.await
      processor ! UnregisterHandler(handler)
      latch.await
      handlers.contains(handler) must equal(false)
    }

    "respond to messages defined in the handlers" in {
      val latch = new CountDownLatch(1)
      val handler = actorOf(new Actor { def receive = { case "a message" => latch.countDown } } ).start
      val handlers = new ConcurrentSkipListSet[ActorRef]((handler :: Nil).toList)
      val processor = actorOf(new ScapuletComponent(conn, handlers, None)).start
      processor ! "a message"
      latch.await(2, TimeUnit.SECONDS)
      latch.getCount must be (0)
    }
    
    "indicate it can respond to messages defined in the handlers" in {
      val handler = actorOf(new Actor { def receive = { case "a message" =>  } }).start
      val handlers = new ConcurrentSkipListSet[ActorRef]((handler :: Nil).toList)
      val processor = actorOf(new ScapuletComponent(conn, handlers, None)).start
      processor.isDefinedAt("a message") must equal(true)
    }

    "indicate it can't respond to messages defined in the handlers" in {
      val handler = actorOf(new Actor { def receive = { case "a message" =>  } }).start
      val handlers = new ConcurrentSkipListSet[ActorRef]((handler :: Nil).toList)
      val processor = actorOf(new ScapuletComponent(conn, handlers, None)).start
      processor.isDefinedAt("the message") must equal(false)
    }
  }

}

// vim: set si ts=2 sw=2 sts=2 et:
