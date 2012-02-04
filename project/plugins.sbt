resolvers += Classpaths.typesafeResolver

resolvers += "Akka Releases" at "http://akka.io/releases/"

resolvers += Resolver.url("sbt-plugin-releases", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

resolvers += "gseitz@github" at "http://gseitz.github.com/maven/"

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.4")

addSbtPlugin("com.typesafe.akka" % "akka-sbt-plugin" % "2.0-M3")

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.3.1")

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.5")

