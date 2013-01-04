/*
 * Copyright 2012 Eike Kettner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import Keys._
import Dependencies._
import org.eknet.publet.sbt._
import sbtassembly.Plugin._
import AssemblyKeys._

object Resolvers {
  val eknet = "eknet.org" at "https://eknet.org/maven2"
  val milton = "milton.io" at "http://milton.io/maven"
  val apacheSnapshots = "apache-snapshots" at "https://repository.apache.org/content/repositories/snapshots/"
}
object Version {
  val bouncyCastle = "1.46"
  val slf4j = "1.7.2"
  val logback = "1.0.9"
  val scalaTest = "2.0.M6-SNAP3"
  val grizzled = "0.6.9"
  val scala = "2.9.2"
  val servlet = "3.0.1"
  val publet = "1.0.1"
  val scue = "0.2.0"
  val james = "3.0-beta5-SNAPSHOT"
  val neoswing = "2.0.0-m1"
}

object Dependencies {

  val publetQuartz = "org.eknet.publet.quartz" %% "publet-quartz" % "0.1.0"  exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet")
  val publetAppDev = "org.eknet.publet" %% "publet-app" % Version.publet  exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet")
  val publetAppPlugin = publetAppDev % "publet"  exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet")
  val publetQuartzPlugin = publetQuartz % "publet" exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet")

  val mail = "javax.mail" % "mail" % "1.4"

  def jamesServer(str: String) = "org.apache.james" % ("james-server-"+ str) %
    Version.james exclude("commons-logging", "commons-logging") exclude("commons-logging", "commons-logging-api")

  val jamesServerCore = jamesServer("core")
  val jamesServerDataApi = jamesServer("data-api")
  val jamesServerDataFile = jamesServer("data-file")
  val jamesServerDataJdbc = jamesServer("data-jdbc")
  val jamesServerDnsLib = jamesServer("dnsservice-library")
  val jamesServerDnsApi = jamesServer("dnsservice-api")
  val jamesServerDnsJava = jamesServer("dnsservice-dnsjava")
  val jamesServerFsApi = jamesServer("filesystem-api")
  val jamesServerLifecycleApi = jamesServer("lifecycle-api")
  val jamesServerMailetApi = jamesServer("mailetcontainer-api")
  val jamesServerMailetCamel = jamesServer("mailetcontainer-camel")
  val jamesServerQueueApi = jamesServer("queue-api")
  val jamesServerMailboxAdapater = jamesServer("mailbox-adapter")
  val jamesServerProtoLib = jamesServer("protocols-library")
  val jamesServerUtil = jamesServer("util")
  val jamesServerDataLib = jamesServer("data-library")
  val jamesServerProtoSmtp = jamesServer("protocols-smtp")
  val jamesServerProtoImap4 = jamesServer("protocols-imap4")
  val jamesServerProtoPop3 = jamesServer("protocols-pop3")
  val jamesServerFetchmail = jamesServer("fetchmail")
  val jamesServerQueueFile = jamesServer("queue-file")
  val jamesServerMailets = jamesServer("mailets")

  // for tests
  val jamesServerDataLibTest = jamesServerDataLib classifier("tests")
  val jamesServerDataApiTest = jamesServerDataApi classifier("tests")
  val jamesServerDnsApiTest = jamesServerDnsApi classifier("tests")

  val jamesServerAll = Seq(jamesServerCore, jamesServerDataApi, jamesServerDataFile, jamesServerDnsLib,
    jamesServerDnsApi, jamesServerDnsJava, jamesServerFsApi, jamesServerLifecycleApi, jamesServerMailetApi,
    jamesServerMailetCamel, jamesServerQueueApi, jamesServerMailboxAdapater, jamesServerProtoLib, jamesServerUtil,
    jamesServerDataLib,  jamesServerProtoSmtp, jamesServerProtoImap4,jamesServerProtoPop3, jamesServerQueueFile,
    jamesServerMailets, jamesServerFetchmail
  )

  val providedDeps = Seq(
    "org.eknet.publet" %% "publet-web" % Version.publet exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet"),
    "org.eknet.publet" %% "publet-webeditor" % Version.publet exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet"),
    "org.eknet.scue" %% "scue" % Version.scue,
    "org.eknet.publet" %% "publet-ext" % Version.publet exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet"),
    "org.slf4j" % "jcl-over-slf4j" % Version.slf4j,
    "org.scalatest" %% "scalatest" % Version.scalaTest,
    "org.clapper" %% "grizzled-slf4j" % Version.grizzled exclude("org.slf4j", "slf4j-api"),
    "org.bouncycastle" % "bcprov-jdk16" % Version.bouncyCastle,
    "org.bouncycastle" % "bcmail-jdk16" % Version.bouncyCastle,
    "javax.servlet" % "javax.servlet-api" % Version.servlet,
    publetQuartz
  ) map (_ % "provided")

  val testDeps = Seq(
    "org.slf4j" % "slf4j-simple" % Version.slf4j,
    "org.eknet.neoswing" % "neoswing" % Version.neoswing,
    "junit" % "junit" % "4.10",
    "org.eknet.scue" %% "scue" % Version.scue classifier("test"),
    jamesServerDataLibTest,
    jamesServerDataApiTest,
    jamesServerDnsApiTest
  ) map (_ % "test")

}

// Root Module 

object RootBuild extends Build {

  lazy val root = Project(
    id = "publet-james",
    base = file("."),
    settings = buildSettings
  )

  lazy val runner = Project(
    id = "publet-runner",
    base = file("runner"),
    settings = Project.defaultSettings ++ Seq(
      name := "publet-runner",
      libraryDependencies ++= Seq(publetAppDev, publetQuartz)
    )
  ) dependsOn (root)

  val exludedFiles = Set(
    "commons-codec-1.5.jar",
    "commons-collections-3.2.1.jar",
    "commons-httpclient-3.0.1.jar",
    "activation-1.1.1.jar",
    "mail-1.4.4.jar",
    "junit-3.8.1.jar",
    "geronimo-javamail_1.4_mail-1.8.3.jar",
    "slf4j-api-1.7.2.jar",
    "slf4j-api-1.6.1.jar",
    "geronimo-annotation_1.0_spec-1.1.1.jar"
  )
  def isExcluded(n: String) = exludedFiles contains (n)

  val buildSettings = Project.defaultSettings ++ assemblySettings ++ ReflectPlugin.allSettings ++ Seq(
    name := "publet-james",
    ReflectPlugin.reflectPackage := "org.eknet.publet.james",
    sourceGenerators in Compile <+= ReflectPlugin.reflect,
    libraryDependencies ++= deps,
    assembleArtifact in packageScala := false,
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      cp filter { f => isExcluded(f.data.getName) }
    }
  ) ++ PubletPlugin.publetSettings

  override lazy val settings = super.settings ++ Seq(
    version := "0.1.0-SNAPSHOT",
    organization := "org.eknet.publet.james",
    scalaVersion := Version.scala,
    exportJars := true,
    publishMavenStyle := true,
    publishTo := Some("eknet-maven2" at "https://eknet.org/maven2"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    pomIncludeRepository := (_ => false),
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    resolvers ++= Seq(Resolvers.eknet, Resolvers.milton),
    licenses := Seq(("ASL2", new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))),
    scmInfo := Some(ScmInfo(new URL("https://eknet.org/gitr/?r=eike/publet-james.git"), "scm:git:https://eknet.org/git/eike/publet-james.git"))
  )

  val deps = Seq(mail, publetAppPlugin, publetQuartzPlugin) ++ jamesServerAll ++ providedDeps ++ testDeps
}


