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
import org.apache.james.smtpserver.netty.{SMTPServerFactory, SMTPServer}
import com.google.inject.Singleton
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.dnsservice.dnsjava.DNSJavaService
import org.eknet.publet.james.data.{RecipientTable, PubletDomainList}

/**
 * This class is a duplicate of james `ConfigurationProviderImpl` which is in a module
 * that needs spring. As I don't want to depend on spring classes, I'm copying (and scalafying)
 * the class to here.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 20.10.12 18:53
 */
@Singleton
class JamesConfigurationProvider {

  private val configFileSuffix = ".conf"
  private val _configs = collection.mutable.Map[String, HierarchicalConfiguration]()

  val configMappings = Map(
    "mailprocessor" -> "mailetcontainer.processors",
    "mailspooler" -> "mailetcontainer.spooler",
    "mailetcontext" -> "mailetcontainer.context"
  )

  val nameMap = Map[Class[_], String](
    classOf[SMTPServerFactory] -> "smtpserver",
    classOf[DNSJavaService] -> "dnsservice",
    classOf[PubletDomainList] -> "domainlist",
    classOf[RecipientTable] -> "recipientrewritetable"
  )

  configMappings.foreach(mapping => {
    registerConfiguration(mapping._1, getConfigByName(mapping._2))
  })

  def registerConfiguration(beanName: String, conf: HierarchicalConfiguration) {
    this._configs.put(beanName, conf)
  }

  def getConfigByName(name: String) = {
    this._configs.get(name) getOrElse {
      val confName = ConfigName(name)
      val conf = loadResource(confName)
        .map(getConfig)
        .map(cfg => confName.part.map(cfg.configurationAt(_)).getOrElse(cfg))
      this._configs.put(name, conf.getOrElse(sys.error("Cannot load configuration: "+name)))
      conf.get
    }
  }

  def getConfiguration(c: Class[_]) = {
    val name = nameMap.get(c).getOrElse(c.getName)
    getConfigByName(name)
  }

  private def loadResource(cn: ConfigName) =
    Option(getClass.getResource("/" + cn.name + configFileSuffix))

  private def getConfig(resource: URL): XMLConfiguration = {
    val xmlconfig = new XMLConfiguration()
    xmlconfig.setDelimiterParsingDisabled(true)
    xmlconfig.setAttributeSplittingDisabled(true)
    xmlconfig.load(resource.openStream())
    xmlconfig
  }

  case class ConfigName(name: String, part:Option[String])
  object ConfigName {
    def apply(str: String): ConfigName = str.indexOf(".") match {
      case i if (i > -1) => ConfigName(str.substring(0, i), Some(str.substring(i+1)))
      case _ => ConfigName(str, None)
    }
  }
}