import sbt._

class ScapuletPlugins(info: ProjectInfo) extends PluginDefinition(info) {
  object Repositories {
    lazy val ScalaTools           = "Scala tools" at "http://maven/content/repositories/scala-releases/"
	}

  import Repositories._
}

// vim: set si ts=2 sw=2 sts=2 et:
