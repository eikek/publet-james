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

package org.eknet.publet.james.config

import com.google.inject.{Inject, Singleton}
import grizzled.slf4j.Logging
import java.io.{FileNotFoundException, InputStream}
import java.net.URL
import org.apache.commons.configuration.{XMLConfiguration, HierarchicalConfiguration}
import org.apache.james.dnsservice.dnsjava.DNSJavaService
import org.apache.james.filesystem.api.FileSystem
import org.apache.james.imapserver.netty.IMAPServerFactory
import org.apache.james.mailetcontainer.impl.camel.CamelCompositeProcessor
import org.apache.james.mailetcontainer.impl.{JamesMailetContext, JamesMailSpooler}
import org.apache.james.smtpserver.netty.SMTPServerFactory
import org.eknet.publet.james.data.{PubletFilesystem, MailRepositoryStoreImpl, RecipientTable, PubletDomainList}
import org.eknet.publet.web.Config
import org.eknet.publet.james.server.{PubletPop3ServerFactory, PubletImapServerFactory, PubletSmtpServerFactory}
import org.apache.james.pop3server.netty.POP3ServerFactory
import org.apache.james.fetchmail.FetchScheduler
import com.google.common.base.{Suppliers, Supplier}
import java.util.concurrent.ConcurrentHashMap

/**
 * This class looks up configuration files for apache services. It will first
 * look at publet's config directory and then fallback to publets and then to
 * james' default configurations.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 20.10.12 18:53
 */
@Singleton
class JamesConfigurationProvider @Inject() (filesystem: FileSystem, conf: Config) extends ConfigurationProvider with Logging {

  private val configFileSuffix = ".conf"
  private val _configs = new ConcurrentHashMap[String, Supplier[HierarchicalConfiguration]]()

  val configMappings = Map(
    "mailprocessor" -> "mailetcontainer.processors",
    "mailspooler" -> "mailetcontainer.spooler",
    "mailetcontext" -> "mailetcontainer.context"
  )

  val nameMap = Map[Class[_], String](
    classOf[SMTPServerFactory] -> "smtpserver",
    classOf[PubletSmtpServerFactory] -> "smtpserver",
    classOf[IMAPServerFactory] -> "imapserver",
    classOf[PubletImapServerFactory] -> "imapserver",
    classOf[POP3ServerFactory] -> "pop3server",
    classOf[PubletPop3ServerFactory] -> "pop3server",
    classOf[DNSJavaService] -> "dnsservice",
    classOf[PubletDomainList] -> "domainlist",
    classOf[RecipientTable] -> "recipientrewritetable",
    classOf[CamelCompositeProcessor] -> "mailprocessor",
    classOf[JamesMailSpooler] -> "mailspooler",
    classOf[JamesMailetContext] -> "mailetcontext",
    classOf[MailRepositoryStoreImpl] -> "mailrepositorystore",
    classOf[FetchScheduler] -> "fetchmail"
  )

  configMappings.foreach(mapping => {
    registerConfiguration(mapping._1, getConfigByName(mapping._2))
  })

  private def fs = filesystem.asInstanceOf[PubletFilesystem]

  def registerConfiguration(beanName: String, conf: HierarchicalConfiguration) {
    postProcessConfig(conf)
    val supp = Suppliers.ofInstance(conf)
    this._configs.put(beanName, supp)
  }

  def getConfigByName(name: String) = synchronized {
    val factory = new Supplier[HierarchicalConfiguration] {
      def get() = {
        val confName = ConfigName(name)
        val config = confName.part.map(part =>
          getConfig(confName.load).configurationAt(part)).getOrElse(getConfig(confName.load))
        config
      }
    }
    val supplier = _configs.putIfAbsent(name, Suppliers.memoize(factory))
    if (supplier != null) supplier.get() else factory.get()
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
    postProcessConfig(xmlconfig)
    xmlconfig
  }

  private def postProcessConfig(xmlconfig: HierarchicalConfiguration) {
    //override with values from publet's config file
    val prefix = "publet.james.conf."
    conf.keySet.map(key => {
      if (key.startsWith(prefix)) {
        val name = key.substring(prefix.length)
        if (xmlconfig.getProperty(name) != null) {
          val value = conf(key).get
          info("Overriding property '"+name+"' from publet.properties")
          xmlconfig.setProperty(name, value)
        }
      }
    })
  }

  case class ConfigName(name: String, part:Option[String]) {

    private val locations = List(
      FileSystem.FILE_PROTOCOL_AND_CONF + name + configFileSuffix,
      "classpath:/org/eknet/publet/james/config/"+ name + configFileSuffix,
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