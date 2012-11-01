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

import org.apache.james.rrt.lib.{RecipientRewriteTableUtil, AbstractRecipientRewriteTable}
import collection.JavaConversions._
import com.google.inject.{Inject, Singleton}
import org.apache.james.rrt.api.RecipientRewriteTable
import java.util

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 12:38
 */
@Singleton
class RecipientTable @Inject() (maildb: MailDb) extends AbstractRecipientRewriteTable {

  def addMappingInternal(user: String, domain: String, mapping: String) {
    maildb.addMapping(user, domain, mapping)
  }

  def removeMappingInternal(user: String, domain: String, mapping: String) {
    maildb.removeMapping(user, domain, mapping)
  }

  def getUserDomainMappingsInternal(user: String, domain: String) =
    maildb.userDomainMappings(user, domain) match {
      case Nil => null
      case x => x
    }

  def getAllMappingsInternal = {
    val map = new util.HashMap[String, util.Collection[String]]()
    maildb.allMappings foreach { t => map.put(t._1, t._2) }
    if (map.isEmpty) null
    else map
  }

  def mapAddressInternal(user: String, domain: String) = {
    maildb.userDomainMappings(user, domain) match {
      case list if (!list.isEmpty) => RecipientRewriteTableUtil.CollectionToMapping(list)
      case _ => maildb.userDomainMappings(user, RecipientRewriteTable.WILDCARD) match {
        case list if (!list.isEmpty) => RecipientRewriteTableUtil.CollectionToMapping(list)
        case _ => maildb.userDomainMappings(RecipientRewriteTable.WILDCARD, domain) match {
          case list if (!list.isEmpty) => RecipientRewriteTableUtil.CollectionToMapping(list)
          case _ => null
        }
      }
    }
  }
}
