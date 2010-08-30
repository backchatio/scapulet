import java.util.jar.Attributes
import java.util.jar.Attributes.Name._
import sbt._
import de.tuxed.codefellow.plugin.CodeFellowPlugin

class ScapuletProject(info: ProjectInfo) extends DefaultProject(info) with CodeFellowPlugin with IdeaProject {

  val AKKA_VERSION = "0.10"
  val ATMO_VERSION = "0.6.1"
  val LIFT_VERSION = "2.1-M1"
  val GRIZZLY_VERSION = "1.9.18-m"
  val SCALATEST_VERSION = "1.2-for-scala-2.8.0.final-SNAPSHOT"


  override def compileOrder = CompileOrder.JavaThenScala

  val publishTo = if(version.toString.endsWith("-SNAPSHOT"))
      ("Mojolly Snapshots" at "http://maven/content/repositories/snapshots/")
    else ("Mojolly Releases" at "http://maven/content/repositories/releases/")
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)


  object Repositories {
    lazy val EmbeddedRepo            = "Embedded Repo" at (info.projectPath / "embedded-repo").asURL.toString
    lazy val ThirdPartySnapshots     = MavenRepository("Mojolly Third Party Snapshots", "http://maven/content/repositories/thirdparty-snapshots/")
    lazy val Maven2JavaNet           = MavenRepository("Mojolly Java.net Maven 2", "http://download.java.net/maven/2/")
    lazy val SonatypeOssReleases     = MavenRepository("Mojolly sonatype oss releases", "http://oss.sonatype.org/content/repositories/releases/")
    lazy val CodehausRepo            = MavenRepository("Mojolly Codehaus Snapshots", "http://repository.codehaus.org")
    lazy val GuiceyFruitRepo         = MavenRepository("Mojolly GuiceyFruit Releases", "http://guiceyfruit.googlecode.com/svn/repo/releases/")
    lazy val JBossRepo               = MavenRepository("Mojolly JBoss releases", "https://repository.jboss.org/nexus/content/groups/public/")
    lazy val SunJDMKRepo             = MavenRepository("Mojolly JDMK Releases", "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo/")
    lazy val AkkaRepository          = MavenRepository("AkkaRepository", "http://www.scalablesolutions.se/akka/repository")
	}

  import Repositories._

  lazy val voldemortModuleConfig        = ModuleConfiguration("voldemort.store.compress", AkkaRepository)
  lazy val scalaTimeModuleConfig        = ModuleConfiguration("org.scala-tools", "time_2.8.0", ThirdPartySnapshots)
  lazy val vscaladocModuleConfig        = ModuleConfiguration("org.scala-tools", "vscaladoc", AkkaRepository)
  lazy val akkaModuleConfig             = ModuleConfiguration("se.scalablesolutions.akka", AkkaRepository)
  lazy val sbinaryModuleConfig          = ModuleConfiguration("sbinary", AkkaRepository)
  lazy val sjsonModuleConfig            = ModuleConfiguration("sjson.json", AkkaRepository)
  lazy val configgyModuleConfig         = ModuleConfiguration("net.lag", "configgy", AkkaRepository)
  lazy val aspectWerkzModuleConfig      = ModuleConfiguration("org.codehaus.aspectwerkz", AkkaRepository)
  lazy val jsr166xModuleConfig          = ModuleConfiguration("jsr166x", AkkaRepository)
  lazy val atmosphereModuleConfig       = ModuleConfiguration("org.atmosphere", SonatypeOssReleases)
  lazy val grizzlyModuleConfig          = ModuleConfiguration("com.sun.grizzly", Maven2JavaNet)
  lazy val guiceyFruitModuleConfig      = ModuleConfiguration("org.guiceyfruit", GuiceyFruitRepo)
  lazy val jbossModuleConfig            = ModuleConfiguration("org.jboss", JBossRepo)
  lazy val facebookModuleConfig         = ModuleConfiguration("com.facebook", AkkaRepository)
  lazy val jdmkModuleConfig             = ModuleConfiguration("com.sun.jdmk", SunJDMKRepo)
  lazy val jerseyContrModuleConfig      = ModuleConfiguration("com.sun.jersey.contribs", Maven2JavaNet)
  lazy val jerseyModuleConfig           = ModuleConfiguration("com.sun.jersey", Maven2JavaNet)
  lazy val jgroupsModuleConfig          = ModuleConfiguration("jgroups", JBossRepo)
  lazy val jmsModuleConfig              = ModuleConfiguration("javax.jms", SunJDMKRepo)
  lazy val jmxModuleConfig              = ModuleConfiguration("com.sun.jmx", SunJDMKRepo)
  lazy val multiverseModuleConfig       = ModuleConfiguration("org.multiverse", CodehausRepo)
  lazy val nettyModuleConfig            = ModuleConfiguration("org.jboss.netty", JBossRepo)
  lazy val redisModuleConfig            = ModuleConfiguration("com.redis", AkkaRepository)
  lazy val scalaTestModuleConfig        = ModuleConfiguration("org.scalatest", ScalaToolsSnapshots)
  lazy val testInterfaceModuleConfig    = ModuleConfiguration("org.scala-tools.testing", "test-interface", ScalaToolsReleases)
  lazy val specsModuleConfig            = ModuleConfiguration("org.scala-tools.testing", "specs_2.8.0", ScalaToolsReleases)
  lazy val scalacheckModuleConfig       = ModuleConfiguration("org.scala-tools.testing", "scalacheck_2.8.0", ScalaToolsSnapshots)
  lazy val embeddedRepo                 = EmbeddedRepo

  val akkaVersion = AKKA_VERSION

  // convenience method
  def akkaModule(module: String) = "se.scalablesolutions.akka" %% ("akka-" + module) % akkaVersion

  // akka core dependency by default
  val akkaCore   = akkaModule("core") //withSources
  val netty = "org.jboss.netty" % "netty" % "3.2.1.Final" withSources
  
  val scalaTime  ="org.scala-tools" %% "time" % "0.2-SNAPSHOT"
  val scalaTest  = "org.scalatest" % "scalatest" % SCALATEST_VERSION % "test"
  val scalaSpecs = "org.scala-tools.testing" %% "specs" % "1.6.5"  % "test"
  val scalaCheck = "org.scala-tools.testing" %% "scalacheck" % "1.8-SNAPSHOT"  % "test"
  val junit      = "junit" % "junit" % "4.5" % "test"
  val mockito    = "org.mockito" % "mockito-all" % "1.8.4" % "test"


    def allArtifacts = {
      Path.fromFile(buildScalaInstance.libraryJar) +++
      (removeDupEntries(runClasspath filter ClasspathUtilities.isArchive)) +++
      ((outputPath ##) / defaultJarName) +++
      mainDependencies.scalaJars
    }

    override def packageOptions =
      manifestClassPath.map(cp => ManifestAttributes(
        (Attributes.Name.CLASS_PATH, cp),
        (IMPLEMENTATION_TITLE, "Scapulet XMPP Component connection"),
        (IMPLEMENTATION_URL, "http://github.com/casualjim/scapulet"),
        (IMPLEMENTATION_VENDOR, "Mojolly Ltd.")
      )).toList

  def removeDupEntries(paths: PathFinder) =
   Path.lazyPathFinder {
     val mapped = paths.get map { p => (p.relativePath, p) }
     (Map() ++ mapped).values.toList
   }

  override def pomExtra =
    <inceptionYear>2010</inceptionYear>
    <url>http://github.com/casualjim/scapulet</url>
    <organization>
      <name>Mojolly</name>
      <url>http://backchat.im</url>
    </organization>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
}

// vim: set si ts=2 sw=2 sts=2 et:
