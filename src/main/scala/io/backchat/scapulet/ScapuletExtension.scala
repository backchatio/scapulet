package io.backchat.scapulet

import com.typesafe.config.Config
import akka.actor._
import akka.dispatch.Await

object ScapuletExtension extends ExtensionId[ScapuletExtension] with ExtensionIdProvider {
  def lookup() = this

  def createExtension(system: ExtendedActorSystem) = new io.backchat.scapulet.ScapuletExtension(system)

  class ComponentSettings(config: Config, defaultConfig: Config) {
    def get(name: String) = if (config.hasPath(name)) {
      Some(ComponentConfig(config.withFallback(defaultConfig)))
    } else None

    def apply(name: String) = get(name).get
  }

  class ScapuletSettings(system: ExtendedActorSystem) {

    //    val eventStream = new StanzaEventBus

    private val cfg = system.settings.config

    val component =
      new ComponentSettings(cfg.getConfig("scapulet.components"), cfg.getConfig("scapulet.components.default"))

  }

  sealed trait ActorFactoryMessage
  case class CreateActor(props: Props, name: String = "") extends ActorFactoryMessage
  case class StopActor(actorRef: ActorRef) extends ActorFactoryMessage

  private class Supervisor extends Actor {
    override val supervisorStrategy = {
      import akka.actor.SupervisorStrategy._
      OneForOneStrategy() {
        case _: ActorKilledException         ⇒ Stop
        case _: ActorInitializationException ⇒ Stop
        case _: Exception                    ⇒ Restart
      }
    }

    protected def receive = {
      case CreateActor(props, null | "") ⇒ sender ! (try context.actorOf(props) catch { case e: Exception ⇒ e })
      case CreateActor(props, name)      ⇒ sender ! (try context.actorOf(props, name) catch { case e: Exception ⇒ e })
      case StopActor(ref)                ⇒ context.stop(ref)
      case Terminated(_)                 ⇒ context stop self
      case m                             ⇒ context.system.deadLetters ! DeadLetter(m, sender, self)
    }
  }

  class Component(val connection: ActorRef, system: ActorSystem) {
    import akka.pattern.ask
    implicit private val timeout = system.settings.ActorTimeout
    private val registration = ComponentConnection.RegisterHandler
    //    def registerHandler(name: String, predicate: Stanza.Predicate, handler: ⇒ ScapuletHandler): ActorRef = {
    //      Await.result((connection ? registration(predicate, Props(handler), name)).mapTo[ActorRef], timeout.duration)
    //    }
    //
    //    def registerHandler(name: String, handle: akka.actor.Actor.Receive): ActorRef = {
    //      val predicate = Stanza.matching(name, { case x ⇒ handle.isDefinedAt(x) })
    //      val handler = () ⇒ new ScapuletHandler {
    //        protected def handleStanza = handle
    //      }
    //      registerHandler(name, predicate, handler())
    //    }
  }

  class ScapuletExtension(system: ExtendedActorSystem) {
    val settings = new ScapuletExtension.ScapuletSettings(system)

    private val guardian = system.actorOf(Props[Supervisor], "xmpp")
    import akka.pattern.ask
    implicit private val timeout = system.settings.ActorTimeout

    private val components = {
      Await.result((guardian ? CreateActor(Props[Supervisor], "components")).mapTo[ActorRef], timeout.duration)
    }

    private[scapulet] def componentConnection(component: XmppComponent) = {
      if (system.actorFor(components.path / component.id).isTerminated)
        Await.result((components ? CreateActor(Props(new ComponentConnection(component)), component.id)).mapTo[ActorRef], timeout.duration)
      else system.actorFor(components.path / component.id)
    }


  }
}
class ScapuletExtension(system: ExtendedActorSystem) extends Extension {

  val scapulet = new ScapuletExtension.ScapuletExtension(system)
}
