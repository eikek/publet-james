/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
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

package org.eknet.publet.james.guice

import org.apache.commons.configuration.{XMLConfiguration, HierarchicalConfiguration}
import java.net.URL
import org.apache.james.smtpserver.netty.SMTPServerFactory
import com.google.inject.{Inject, Singleton}
import org.apache.james.dnsservice.dnsjava.DNSJavaService
import org.eknet.publet.james.data.{PubletFilesystem, MailRepositoryStoreImpl, RecipientTable, PubletDomainList}
import org.apache.james.imapserver.netty.IMAPServerFactory
import org.apache.james.mailetcontainer.impl.camel.CamelCompositeProcessor
import org.apache.james.mailetcontainer.impl.{JamesMailetContext, JamesMailSpooler}
import org.eknet.publet.web.Config
import org.apache.james.filesystem.api.FileSystem
import java.io.{FileNotFoundException, InputStream}
import grizzled.slf4j.Logging

/**
 * This class looks up configuration files for apache services. It will first
 * look at publet's config directory and then fallback to publets and then to
 * james' default configurations.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 20.10.12 18:53
 */
@Singleton
class JamesConfigurationProvider @Inject() (filesystem: FileSystem) extends ConfigurationProvider with Logging {

  private val configFileSuffix = ".conf"
  private val _configs = collection.mutable.Map[String, HierarchicalConfiguration]()

  val configMappings = Map(
    "mailprocessor" -> "mailetcontainer.processors",
    "mailspooler" -> "mailetcontainer.spooler",
    "mailetcontext" -> "mailetcontainer.context"
  )

  val nameMap = Map[Class[_], String](
    classOf[SMTPServerFactory] -> "smtpserver",
    classOf[IMAPServerFactory] -> "imapserver",
    classOf[DNSJavaService] -> "dnsservice",
    classOf[PubletDomainList] -> "domainlist",
    classOf[RecipientTable] -> "recipientrewritetable",
    classOf[CamelCompositeProcessor] -> "mailprocessor",
    classOf[JamesMailSpooler] -> "mailspooler",
    classOf[JamesMailetContext] -> "mailetcontext",
    classOf[MailRepositoryStoreImpl] -> "mailrepositorystore"
  )

  configMappings.foreach(mapping => {
    registerConfiguration(mapping._1, getConfigByName(mapping._2))
  })

  private def fs = filesystem.asInstanceOf[PubletFilesystem]

  def registerConfiguration(beanName: String, conf: HierarchicalConfiguration) {
    this._configs.put(beanName, conf)
  }

  def getConfigByName(name: String) = synchronized {
    this._configs.get(name) getOrElse {
      val confName = ConfigName(name)
      val config = confName.part.map(part =>
        getConfig(confName.load).configurationAt(part)).getOrElse(getConfig(confName.load))
      this._configs.put(name, config)
      config
    }
  }

  def getConfiguration(c: Class[_]) = synchronized {
    val name = nameMap.get(c).getOrElse(c.getName)
    getConfigByName(name)
  }

  private def getConfig(in: InputStream): XMLConfiguration = {
    val xmlconfig = new XMLConfiguration()
    xmlconfig.setDelimiterParsingDisabled(true)
    xmlconfig.setAttributeSplittingDisabled(true)
    xmlconfig.load(in)
    in.close()
    xmlconfig
  }

  case class ConfigName(name: String, part:Option[String]) {

    private val locations = List(
      FileSystem.FILE_PROTOCOL_AND_CONF + name + configFileSuffix,
      "classpath:/org/eknet/publet/james/conf/"+ name + configFileSuffix,
      "classpath:/"+ name + configFileSuffix
    )


    def load = {
      def loadRecursive(urls: List[String]): InputStream = urls match {
        case a::as => fs.findResource(a) match {
          case Some(in) => debug("Found resource "+ a); in
          case _ => loadRecursive(as)
        }
        case _ => throw new FileNotFoundException("Config not found: "+ this)
      }
      loadRecursive(locations)
    }
  }

  object ConfigName {
    def apply(str: String): ConfigName = str.indexOf(".") match {
      case i if (i > -1) => ConfigName(str.substring(0, i), Some(str.substring(i+1)))
      case _ => ConfigName(str, None)
    }
  }
}