import sbt._

class ScapuletPlugins(info: ProjectInfo) extends PluginDefinition(info) {
  object Repositories {
    lazy val ScalaTools           = "Scala tools" at "http://maven/content/repositories/scala-releases/"
    
	}
  val mojollyThirdParth = "Mojolly ThirdParty" at "http://maven/content/repositories/thirdparty"
  import Repositories._
  val codefellow = "de.tuxed" % "codefellow_2.8.0" % "0.4"
}

// vim: set si ts=2 sw=2 sts=2 et:
