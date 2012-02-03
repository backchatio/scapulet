package io.backchat.scapulet

import akka.actor.ActorSystem
import akka.event.LoggingAdapter

trait Logging {
  
  protected implicit def system: ActorSystem
  @transient lazy val logger: LoggingAdapter = akka.event.Logging(system, getClass)
}
