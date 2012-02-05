package io.backchat.scapulet

import akka.event.{ ActorEventBus, EventBus }
import collection.JavaConversions._
import com.google.common.collect.MapMaker
import xml.{ Elem, Node }

object StanzaEventBus {

  trait StanzaPredicate {
    def name: String
    def apply(evt: Elem): Boolean
    override def equals(obj: Any) = obj match {
      case pred: StanzaPredicate ⇒ pred.name == name
      case _                     ⇒ false
    }
  }

  object AllStanzas extends StanzaPredicate {
    val name = "all-stanzas"
    def apply(evt: Elem) = true
  }

  class CompositePredicate(predicate: StanzaPredicate, predicates: StanzaPredicate*) extends StanzaPredicate {
    private val allPredicates = Vector((predicate +: predicates): _*)
    val name = allPredicates sortBy (_.name) mkString "::"

    def apply(evt: Elem) = allPredicates forall (_ apply evt)
  }

  private val mapMaker = new MapMaker
}
class StanzaEventBus extends EventBus with ActorEventBus {
  type Event = Elem
  type Classifier = StanzaEventBus.StanzaPredicate

  private val subscriptions = StanzaEventBus.mapMaker.makeMap[Classifier, Set[Subscriber]].withDefaultValue(Set.empty[Subscriber])

  def subscribe(subscriber: Subscriber, to: Classifier) = {
    val subs = subscriptions(to)
    val res = subs contains subscriber
    subscriptions(to) = subs + subscriber
    res
  }

  def unsubscribe(subscriber: Subscriber, from: Classifier) = {
    val subs = subscriptions(from)
    val res = subs contains subscriber
    subscriptions(from) = subs filterNot (_ == subscriber)
    res
  }

  def unsubscribe(subscriber: Subscriber) {
    subscriptions filter ({ case (_, subs) ⇒ subs contains subscriber }) foreach {
      case (to, subs) ⇒ subscriptions(to) = subs filterNot (_ == subscriber)
    }
  }

  def publish(event: Event) {
    (subscriptions filterKeys (_ apply event)).values.flatten foreach (_ ! event)
  }
}
