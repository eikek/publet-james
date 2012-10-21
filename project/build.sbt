resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "eknet.org" at "https://eknet.org/maven2"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0")

libraryDependencies += "org.eknet.publet" %% "publet-app" % "1.0.0-SNAPSHOT"

addSbtPlugin("org.eknet.publet" % "publet-sbt-plugin" % "1.0.0-SNAPSHOT")
