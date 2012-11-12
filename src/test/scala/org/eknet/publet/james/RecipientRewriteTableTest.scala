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

package org.eknet.publet.james

import org.apache.james.rrt.lib.{AbstractRecipientRewriteTable, AbstractRecipientRewriteTableTest}
import org.slf4j.LoggerFactory
import java.util
import org.scalatest.junit.AssertionsForJUnit
import org.eknet.publet.james.data.{MailDb, RecipientTable}
import org.apache.james.domainlist.api.DomainList
import org.eknet.publet.ext.graphdb.GraphDb

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 31.10.12 19:00
 */
class RecipientRewriteTableTest extends AbstractRecipientRewriteTableTest with AssertionsForJUnit {
  val logger = LoggerFactory.getLogger(classOf[RecipientRewriteTableTest])

  val REGEX_TYPE: Int = 0
  val ERROR_TYPE: Int = 1
  val ADDRESS_TYPE: Int = 2
  val ALIASDOMAIN_TYPE: Int = 3

  type MappingFun = (AbstractRecipientRewriteTable, String, String, String) => Unit

  def function(mappingType: Int, remove: Boolean): MappingFun = {
    (mappingType, remove) match {
      case (REGEX_TYPE, x) => (rrt, user, domain, mapping) => {
        if (x) rrt.removeRegexMapping(user, domain, mapping)
        else rrt.addRegexMapping(user, domain, mapping)
      }
      case (ERROR_TYPE, x) => (rrt, user, domain, mapping) => {
        if (x) rrt.removeErrorMapping(user, domain, mapping)
        else rrt.addErrorMapping(user, domain, mapping)
      }
      case (ALIASDOMAIN_TYPE, x) => (rrt, user, domain, mapping) => {
        if (x) rrt.removeAliasDomainMapping(domain, mapping)
        else rrt.addAliasDomainMapping(domain, mapping)
      }
      case (ADDRESS_TYPE, x) => (rrt, user, domain, mapping) => {
        if (x) rrt.removeAddressMapping(user, domain, mapping)
        else rrt.addAddressMapping(user, domain, mapping)
      }
    }
  }

  def getRecipientRewriteTable = {
    val provider = new TestGraphDbProvider
    val rrt = new RecipientTable(new MailDb(new GraphDb(provider.getNext)))
    rrt.setLog(logger)
    rrt.setDomainList(new DomainList {
      def getDomains = Array("mydomain.com")
      def getDefaultDomain = "mydomain.com"
      def containsDomain(domain: String) = domain == "mydomain.com"
      def removeDomain(domain: String) {}
      def addDomain(domain: String) {}
    })
    rrt.setMappingLimit(10)
    rrt.setRecursiveMapping(true)
    rrt
  }

  def addMapping(user: String, domain: String, mapping: String, mappingType: Int) = {
    val rrt = virtualUserTable
    function(mappingType, remove = false)(rrt, user, domain, mapping)
    true
  }
  def removeMapping(user: String, domain: String, mapping: String, mappingType: Int) = {
    val rrt = virtualUserTable
    function(mappingType, remove = true)(rrt, user, domain, mapping)
    true
  }
}
