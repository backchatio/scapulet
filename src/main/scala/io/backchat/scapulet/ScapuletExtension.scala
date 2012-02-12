package io.backchat.scapulet

import com.typesafe.config.Config
import akka.actor._
import akka.dispatch.Await
import java.util.concurrent.ConcurrentHashMap

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

  class ScapuletExtension(system: ExtendedActorSystem) {
    val settings = new ScapuletExtension.ScapuletSettings(system)

    private val guardian = system.actorOf(Props[Supervisor], "xmpp")
    import akka.pattern.ask

    implicit private val timeout = system.settings.ActorTimeout

    private val components = {
      Await.result((guardian ? CreateActor(Props[Supervisor], "components")).mapTo[ActorRef], timeout.duration)
    }

    private[scapulet] def componentConnection(component: XmppComponent, overrideConfig: Option[ComponentConfig] = None, callback: Option[ActorRef] = None) = {
      Await.result((components ? CreateActor(Props(new ComponentConnection(component, overrideConfig, callback)), component.id)).mapTo[ActorRef], timeout.duration)
    }

    def component(id: String) = {
      require(system.settings.config.hasPath("scapulet.components." + id), "You must define the component in the configuration")
      val comp = system.actorFor(components.path / id)
      if (comp.isTerminated) {
        XmppComponent(id)(system)
      } else Await.result((comp ? ComponentConnection.Component).mapTo[XmppComponent], timeout.duration)
    }

  }
}
class ScapuletExtension(system: ExtendedActorSystem) extends Extension {

  val scapulet = new ScapuletExtension.ScapuletExtension(system)
}
