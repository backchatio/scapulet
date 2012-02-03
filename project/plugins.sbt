resolvers += Classpaths.typesafeResolver

resolvers += "Akka Releases" at "http://akka.io/releases/"

addSbtPlugin("com.typesafe.akka" % "akka-sbt-plugin" % "2.0-M3")

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.3.1")
