resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "eknet.org" at "https://eknet.org/maven2"

addSbtPlugin("org.eknet.publet" % "publet-sbt-plugin" % "1.0.0-SNAPSHOT")
