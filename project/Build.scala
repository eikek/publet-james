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
import Classpaths.managedJars

object Resolvers {
  val eknet = "eknet.org" at "https://eknet.org/maven2"
  val apacheSnapshots = "apache-snapshots" at "https://repository.apache.org/content/repositories/snapshots/"
}
object Version {
  val slf4j = "1.6.4"
  val logback = "1.0.1"
  val scalaTest = "1.8"
  val grizzled = "0.6.9"
  val scala = "2.9.2"
  val servlet = "3.0.1"
  val publet = "1.0.0-SNAPSHOT"
  val james = "3.0-beta4"
  val jamesMailbox = "0.4" //used in james-server
  val standardMailets = "1.1"
}

object Dependencies {

  val junit = "junit" % "junit" % "4.10" % "test"
  val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % Version.grizzled % "provided" withSources() //scala 2.9.2 only
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test" withSources()
  val publetApp = "org.eknet.publet" %% "publet-app" % Version.publet % "publet"
  val publetWeb = "org.eknet.publet" %% "publet-web" % Version.publet % "provided" withSources()
  val publetExt = "org.eknet.publet" %% "publet-ext" % Version.publet % "provided" withSources()
  val servletApi = "javax.servlet" % "javax.servlet-api" % Version.servlet % "provided" withSources()

  def jamesServer(str: String) = "org.apache.james" % ("james-server-"+ str) % Version.james

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

  val jamesDeps = Seq(
    //"commons-daemon" % "commons-daemon" % "1.0.10",
    "org.apache.james" % "apache-james-mailbox-tool" % Version.jamesMailbox
  )

  // for tests
  val jamesServerDataLibTest = jamesServerDataLib % "test" classifier("tests") withSources()

  val jamesServerAll = jamesDeps ++ Seq(jamesServerCore, jamesServerDataApi, jamesServerDataFile, jamesServerDnsLib,
    jamesServerDnsApi, jamesServerDnsJava, jamesServerFsApi, jamesServerLifecycleApi, jamesServerMailetApi,
    jamesServerMailetCamel, jamesServerQueueApi, jamesServerMailboxAdapater, jamesServerProtoLib, jamesServerUtil,
    jamesServerDataLib,  jamesServerProtoSmtp, jamesServerProtoImap4,jamesServerProtoPop3, jamesServerQueueFile,
    jamesServerMailets,
    jamesServerDataLibTest
  )
}

// Root Module 

object RootBuild extends Build {

  lazy val root = Project(
    id = "publet-james",
    base = file("."),
    settings = buildSettings
  )

  val buildSettings = Project.defaultSettings ++ Seq(
    name := "publet-james",
    libraryDependencies ++= deps
  ) ++ PubletPlugin.publetSettings

  override lazy val settings = super.settings ++ Seq(
    version := "1.0.0-SNAPSHOT",
    organization := "org.eknet.publet.james",
    scalaVersion := Version.scala,
    exportJars := true,
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    resolvers ++= Seq(Resolvers.eknet, Resolvers.apacheSnapshots),
    pomExtra := <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
  )

  val deps = Seq(publetWeb, publetExt, publetApp, servletApi, grizzledSlf4j, junit, scalaTest) ++ jamesServerAll
}


