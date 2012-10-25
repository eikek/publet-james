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

package org.eknet.publet.james.data

import org.apache.james.mailrepository.api.{MailRepository, MailRepositoryStore}
import org.apache.james.lifecycle.api.Configurable
import org.apache.commons.configuration._
import javax.annotation.PostConstruct
import grizzled.slf4j.Logging
import collection.mutable
import com.google.common.collect.{Lists, MapMaker}
import java.util
import collection.JavaConversions._
import org.apache.james.mailrepository.api.MailRepositoryStore.MailRepositoryStoreException
import com.google.inject.{Injector, Inject, Singleton}
import org.eknet.publet.james.guice.JamesTypeListener

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.10.12 20:16
 */
@Singleton
class MailRepositoryStoreImpl @Inject() (injector: Injector) extends MailRepositoryStore with Configurable with Logging {

  private val initializer = new JamesTypeListener
  private var _config: HierarchicalConfiguration = _

  private val repositories:util.Map[String, MailRepository] = new MapMaker().softValues().makeMap()
  private val classes = mutable.Map[String, String]()
  private val defaultConfigs = mutable.Map[String, HierarchicalConfiguration]()

  def configure(config: HierarchicalConfiguration) {
    this._config = config
  }

  @PostConstruct
  def init() {
    synchronized {
      info("JamesMailStore init...")
      val registeredClasses = this._config.configurationsAt("mailrepositories.mailrepository")
        .toList.asInstanceOf[List[HierarchicalConfiguration]]

      registeredClasses.foreach(registerRepo)
    }
  }

  private def registerRepo(config: HierarchicalConfiguration) {
    val className = config.getString("[@class]")
    val protocols = config.getList("protocols.protocol").toList.asInstanceOf[List[String]]

    for (protocol <- protocols) {

      info("Registering Repository instance of class "+ className +
        " to handle '"+ protocol +"' protocol requests")

      if (classes.contains(protocol)) {
        throw new ConfigurationException("The combination of type and protocol comprise " +
          "a unqiue key, which has been violated.  Please check your repository configuration.")
      }
      classes.put(protocol, className)

      if (config.getKeys("config").hasNext) {
        defaultConfigs.put(protocol, config.configurationAt("config"))
      }
    }
  }

  def select(url: String): MailRepository = synchronized {

    val protocol = (url, url.indexOf(":")) match {
      case (p, i) if (i > -1) => p.substring(0, i)
      case _ => throw new MailRepositoryStoreException("Destination is malformed: "+ url)
    }
    synchronized {
      Option(repositories.get(url)).getOrElse {
        val config = new CombinedConfiguration()
        defaultConfigs.get(protocol).foreach(config.addConfiguration)
        val builder = new DefaultConfigurationBuilder()
        builder.addProperty("[@destinationURL]", url)
        config.addConfiguration(builder)

        try {
          val clazz = getClass.getClassLoader.loadClass(classes.get(protocol).get)
          val repo = injector.getInstance(clazz).asInstanceOf[MailRepository]
          repo match {
            case conf: Configurable => conf.configure(config)
            case _ =>
          }
          initializer.initialize(repo)
          repositories.put(url, repo)
          info("Added repository '"+ url+ "' -> '"+ classes.get(protocol).get)
          repo
        } catch {
          case e: Exception => {
            error("Exception while creating repository!", e)
            throw new MailRepositoryStoreException("Cannot find or init repository", e)
          }
        }
      }
    }
  }

  def getUrls = Lists.newArrayList(repositories.keySet())
}
