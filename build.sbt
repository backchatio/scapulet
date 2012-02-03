
name := "scapulet"

version := "0.5.1-SNAPSHOT"

organization := "io.backchat.scapulet"

scalaVersion := "2.9.1"

javacOptions ++= Seq("-Xlint:unchecked", "-source", "1.7", "-Xlint:deprecation")

scalacOptions ++= Seq("-optimize", "-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-P:continuations:enable")

autoCompilerPlugins := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-file-mailbox" % "2.0-M3",
  "com.typesafe.akka" % "akka-slf4j" % "2.0-M3" % "provided",
  "com.typesafe.akka" % "akka-testkit" % "2.0-M3" % "test",
  compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.1"),
  compilerPlugin("org.scala-tools.sxr" % "sxr_2.9.0" % "0.2.7"),
  "junit" % "junit" % "4.10" % "test",
  "org.specs2" %% "specs2" % "1.7.1" % "test"
)

homepage := Some(new URL("http://www.scalatra.org/"))

startYear := Some(2010)

licenses := Seq(("BSD", new URL("http://github.com/mojolly/scapulet/raw/HEAD/LICENSE")))

pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
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
    <developer>
      <id>sdb</id>
      <name>Stefan De Boey</name>
      <url>http://ellefant.be/</url>
    </developer>
  </developers>  
)}

packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }