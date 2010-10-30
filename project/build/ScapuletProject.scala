import java.util.jar.Attributes
import java.util.jar.Attributes.Name._
import sbt._

class ScapuletProject(info: ProjectInfo) extends DefaultProject(info) with de.tuxed.codefellow.plugin.CodeFellowPlugin {

  val AKKA_VERSION = "0.17-SNAPSHOT"
  val ATMO_VERSION = "0.7-SNAPSHOT"
  val LIFT_VERSION = "2.1"
  val SCALATEST_VERSION = "1.2"


  override def compileOrder = CompileOrder.JavaThenScala
  override def parallelExecution = true

  override def managedStyle = ManagedStyle.Maven
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
    lazy val JBossRepo               = MavenRepository("Mojolly JBoss releases", "http://maven/content/repositories/jboss/")
    lazy val SunJDMKRepo             = MavenRepository("Mojolly JDMK Releases", "http://wp5.e-taxonomy.eu/cdmlib/mavenrepo/")
    lazy val AkkaRepository          = MavenRepository("AkkaRepository", "http://www.scalablesolutions.se/akka/repository")
    lazy val MojollyReleases        = MavenRepository("Mojolly Releases", "http://maven/content/repositories/releases")
    lazy val MojollySnapshots       = MavenRepository("Mojolly Snapshots", "http://maven/content/repositories/snapshots")
    lazy val CasbahRepoReleases     = MavenRepository("Casbah Release Repo", "http://maven/content/repositories/bumnetworks/")
    lazy val ZookeeperRepo          = MavenRepository("Zookeeper Repo", "http://maven/content/repositories/zookeeper-releases/")
    lazy val ClojarsRepo            = MavenRepository("Clojars Repo", "http://maven/content/repositories/clojar-releases/")
    lazy val ThirdParty             = MavenRepository("Third Party Repo", "http://maven/content/repositories/thirdparty")
    lazy val CasbahSnapshotRepo     = MavenRepository("Casbah Snapshots", "http://maven/content/repositories/bumnetworks-snapshots/")
    lazy val ScalateSnapshots       = MavenRepository("Scalate Snapshots", "http://maven/content/repositories/scalate-snapshots/")
	}

  import Repositories._
  override def repositories = Set("Mojolly Maven Central" at "http://maven/content/repositories/central/")

  lazy val uuidModuleConfig             = ModuleConfiguration("com.eaio", ThirdParty)
  lazy val voldemortModuleConfig        = ModuleConfiguration("voldemort.store.compress", AkkaRepository)
  lazy val scalaTimeModuleConfig        = ModuleConfiguration("org.scala-tools", "time_2.8.0", ThirdPartySnapshots)
  lazy val vscaladocModuleConfig        = ModuleConfiguration("org.scala-tools", "vscaladoc", AkkaRepository)
  lazy val akkaModuleConfig             = ModuleConfiguration("akka", ThirdPartySnapshots)
  lazy val sbinaryModuleConfig          = ModuleConfiguration("sbinary", AkkaRepository)
  lazy val sjsonModuleConfig            = ModuleConfiguration("sjson.json", ThirdParty)
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
  lazy val scalaTestModuleConfig        = ModuleConfiguration("org.scalatest", ScalaToolsReleases)
  lazy val testInterfaceModuleConfig    = ModuleConfiguration("org.scala-tools.testing", "test-interface", ScalaToolsReleases)
  lazy val scalacheckModuleConfig       = ModuleConfiguration("org.scala-tools.testing", "scalacheck_2.8.0", ScalaToolsSnapshots)
  lazy val embeddedRepo                 = EmbeddedRepo

  val akkaVersion = AKKA_VERSION

  lazy val scap_core   = project("core", "scapulet-core", new ScapuletCoreProject(_))
  lazy val scap_pubsub = project("pubsub", "scapulet-pubsub", new ScapuletPubSubProject(_), scap_core)

  // convenience method
  def akkaModule(module: String) = "akka" %% ("akka-" + module) % akkaVersion
  override def deliverProjectDependencies = Nil

  class ScapuletCoreProject(info: ProjectInfo) extends DefaultProject(info) with ScapuletSubProject {

    val description = "The core Scapulet library essentials for connecting to servers"
  
    // akka core dependency by default
    val akkaCore   = akkaModule("remote") //withSources
    val idn = "org.gnu.inet" % "libidn" % "1.15"
    val scalaTime  ="org.scala-tools" %% "time" % "0.2-SNAPSHOT"

  }

  class ScapuletPubSubProject(info: ProjectInfo) extends DefaultProject(info) with ScapuletSubProject {

    def description = "Contains the stanza implementations etc for creating pubsub components and bots"
  
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
        (IMPLEMENTATION_URL, "http://github.com/mojolly/scapulet"),
        (IMPLEMENTATION_VENDOR, "Mojolly Ltd.")
      )).toList

    def removeDupEntries(paths: PathFinder) =
     Path.lazyPathFinder {
       val mapped = paths.get map { p => (p.relativePath, p) }
       (Map() ++ mapped).values.toList
     }

  }

  trait ScapuletSubProject extends BasicScalaProject with BasicPackagePaths with de.tuxed.codefellow.plugin.CodeFellowPlugin { self: BasicScalaProject => 

    def description: String
    
    val scalaTest  = "org.scalatest" % "scalatest" % SCALATEST_VERSION % "test"
    val scalaCheck = "org.scala-tools.testing" %% "scalacheck" % "1.8-SNAPSHOT"  % "test"
    val junit      = "junit" % "junit" % "4.5" % "test"
    val mockito    = "org.mockito" % "mockito-all" % "1.8.5" % "test"


    override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList
    override def compileOrder = CompileOrder.JavaThenScala

    override def pomExtra = (
      <parent>
        <groupId>{organization}</groupId>
        <artifactId>{ScapuletProject.this.artifactID}</artifactId>
        <version>{version}</version>
      </parent>
      <name>{name}</name>
      <description>{description}</description>)
    

    override def packageDocsJar = defaultJarPath("-javadoc.jar")
    override def packageSrcJar= defaultJarPath("-sources.jar")

    // If these aren't lazy, then the build crashes looking for
    // ${moduleName}/project/build.properties.
    lazy val sourceArtifact = Artifact.sources(artifactID)
    lazy val docsArtifact = Artifact.javadoc(artifactID)
    override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)

    override def compileOptions = Unchecked :: Deprecation :: (super.compileOptions ++
      Seq("-Xmigration",
        "-Xcheckinit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))).toList

  }

  trait UnpublishedProject extends BasicManagedProject {
    override def publishLocalAction = task { None }
    override def deliverLocalAction = task { None }
    override def publishAction = task { None }
    override def deliverAction = task { None }
    override def artifacts = Set.empty    
  }

  override def pomExtra =
    <name>{name}</name>
    <description>Scapulet Project POM</description>
    <inceptionYear>2010</inceptionYear>
    <url>http://github.com/mojolly/scapulet</url>
    <organization>
      <name>Mojolly</name>
      <url>http://mojolly.com</url>
    </organization>
    <licenses>
      <license>
        <name>BSD</name>
        <url>http://github.com/mojolly/scapulet/raw/HEAD/LICENSE</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <mailingLists>
      <mailingList>
        <name>Scapulet user group</name>
        <archive>http://groups.google.com/group/scapulet-user</archive>
        <post>scapulet-user@googlegroups.com</post>
        <subscribe>scapulet-user+subscribe@googlegroups.com</subscribe>
        <unsubscribe>scapulet-user+unsubscribe@googlegroups.com</unsubscribe>
      </mailingList>
    </mailingLists>
    <scm>
      <connection>scm:git:git://github.com/mojolly/scapulet.git</connection>
      <url>http://github.com/mojolly/scapulet</url>
    </scm>
    <developers>
      <developer>
        <id>casualjim</id>
        <name>Ivan Porto Carrero</name>
        <url>http://flanders.co.nz/</url>
      </developer>
    </developers>
}

// vim: set si ts=2 sw=2 sts=2 et:
