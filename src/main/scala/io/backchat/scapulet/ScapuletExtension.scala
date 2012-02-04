package io.backchat.scapulet

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }

object ScapuletExtension extends ExtensionId[ScapuletExtension] with ExtensionIdProvider {
  def lookup() = this

  def createExtension(system: ExtendedActorSystem) = new ScapuletExtension(system)

  class ScapuletSettings(system: ExtendedActorSystem) {

  }
}
class ScapuletExtension(system: ExtendedActorSystem) extends Extension {

  val scapulet = new ScapuletExtension.ScapuletSettings(system)
}
