package io.backchat.scapulet

import akka.event.{ ActorEventBus, EventBus }
import xml._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

object Stanza {
  trait Predicate {
    def name: String
    def apply(evt: NodeSeq): Boolean
    override def equals(obj: Any) = obj match {
      case pred: Predicate ⇒ pred.name == name
      case _               ⇒ false
    }

    def &&(other: Predicate): Predicate = {
      new matching.ForAllPredicate(this, other)
    }

    def ||(other: Predicate): Predicate = {
      new matching.ForAnyPredicate(this, other)
    }
  }

  object matching {

    object AllStanzas extends Predicate {
      val name = "all-stanzas"
      def apply(evt: NodeSeq) = true

      override def &&(other: Predicate): Predicate = other

      override def ||(other: Predicate): Predicate = this
    }

    private[Stanza] class ForAllPredicate(predicate: Predicate, predicates: Predicate*) extends Predicate {
      private val allPredicates = Vector((predicate +: predicates): _*)
      val name = allPredicates sortBy (_.name) mkString "::"

      def apply(evt: NodeSeq) = allPredicates forall (_ apply evt)
    }

    private[Stanza] class ForAnyPredicate(predicate: Predicate, predicates: Predicate*) extends Predicate {
      private val allPredicates = Vector((predicate +: predicates): _*)
      val name = allPredicates sortBy (_.name) mkString "||"

      def apply(evt: NodeSeq) = allPredicates exists (_ apply evt)
    }

    private[Stanza] class PartialFunctionPredicate(val name: String, pf: PartialFunction[NodeSeq, Boolean]) extends Predicate {
      def apply(evt: NodeSeq) = pf.isDefinedAt(evt) && pf(evt)
    }

    def apply(name: String, pf: PartialFunction[NodeSeq, Boolean]): Predicate =
      new PartialFunctionPredicate(name, pf)

  }

}
class StanzaEventBus extends EventBus with ActorEventBus {
  type Event = NodeSeq
  type Classifier = Stanza.Predicate

  private val subscriptions = new ConcurrentHashMap[Classifier, Set[Subscriber]]

  def subscribe(subscriber: Subscriber, to: Classifier) = {
    val subs = Option(subscriptions get to) getOrElse Set.empty[Subscriber]
    val res = subs contains subscriber
    if (!res) subscriptions.put(to, subs + subscriber)
    res
  }

  def unsubscribe(subscriber: Subscriber, from: Classifier) = {
    Option(subscriptions get from) map { subs ⇒
      val res = subs contains subscriber
      if (res) subscriptions.put(from, subs filterNot (_ == subscriber))
      res
    } getOrElse false
  }

  def unsubscribe(subscriber: Subscriber) {
    (safeSubscriptions
      filter ({ case (_, subs) ⇒ subs contains subscriber })
      foreach {
        case (to, subs) if subs.nonEmpty ⇒ subscriptions.put(to, subs filterNot (_ == subscriber))
      })
  }

  private def safeSubscriptions = subscriptions.asScala.withDefaultValue(Set.empty[Subscriber])

  def publish(event: Event) {
    val subscribers = (safeSubscriptions filterKeys (_ apply event)).values.flatten

    subscribers foreach (_ ! event)
  }
}
