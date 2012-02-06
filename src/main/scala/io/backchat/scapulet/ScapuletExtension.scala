package io.backchat.scapulet

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import com.typesafe.config.Config
import io.backchat.scapulet.ScapuletExtension.ComponentSettings

object ScapuletExtension extends ExtensionId[ScapuletExtension] with ExtensionIdProvider {
  def lookup() = this

  def createExtension(system: ExtendedActorSystem) = new ScapuletExtension(system)

  class ComponentSettings(config: Config) {
    def get(name: String) = if (config.hasPath(name)) {
      Some(ConnectionConfig(config))
    } else None

    def apply(name: String) = get(name).get
  }

  class ScapuletSettings(system: ExtendedActorSystem) {

    val eventStream = new StanzaEventBus

    private val cfg = system.settings.config

    val components =
      new ComponentSettings(cfg.getConfig("scapulet.components").withFallback(cfg.getConfig("scapulet.components.default")))
  }
}
class ScapuletExtension(system: ExtendedActorSystem) extends Extension {

  val scapulet = new ScapuletExtension.ScapuletSettings(system)
}
