import sbt._

class ScapuletPlugins(info: ProjectInfo) extends PluginDefinition(info) {
  object Repositories {
    lazy val EmbeddedRepo         = "Embedded Repo" at (info.projectPath / "embedded-repo").asURL.toString
    lazy val ScalaTools           = "Scala tools" at "http://maven/content/repositories/scala-releases/"
    lazy val ThirdPartySnapshots  = "Mojolly Third Party Snapshots" at "http://maven/content/repositories/thirdparty-snapshots/"
    lazy val MojollyReleasesRepo  = "Mojolly Releases Repo" at "http://maven/content/repositories/releases"
    lazy val GridDynamics         = "GridDynamics" at "http://maven/content/repositories/griddynamics/"
    lazy val Clapper              = "Clapper" at "http://maven/content/repositories/clapper/"
    lazy val Jawsy                = "Jawsy" at "http://maven/content/repositories/jawsy/"
	}

  import Repositories._

  lazy val jakartaRegexpModuleConfig = ModuleConfiguration("jakarta-regexp", GridDynamics)
  lazy val bcelModuleConfig          = ModuleConfiguration("org.apache.bcel", GridDynamics )
  lazy val codeFellowModuleConfig    = ModuleConfiguration("de.tuxed", MojollyReleasesRepo)
  lazy val ideaModuleConfig          = ModuleConfiguration("com.github.mpeltonen", ThirdPartySnapshots)

  // VIM code completion
  lazy val bcel = "org.apache.bcel" % "bcel" % "5.2"
  lazy val codefellow = "de.tuxed" % "codefellow-plugin" % "0.3"
  lazy val idea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.1-SNAPSHOT"
}

// vim: set si ts=2 sw=2 sts=2 et:
