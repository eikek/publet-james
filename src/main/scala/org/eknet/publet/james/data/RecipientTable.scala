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
import org.apache.james.rrt.api.RecipientRewriteTableException
import org.eknet.publet.web.Config

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 12:38
 */
@Singleton
class RecipientTable @Inject() (config: Config) extends AbstractRecipientRewriteTable {
  private val mappings = Map(
    "superadmin@localhost" -> "superadmin",
    "superadmin@zuub.com" -> "superadmin"
  )

  def addMappingInternal(user: String, domain: String, mapping: String) {
    throw new RecipientRewriteTableException("Read-Only implementation")
  }

  def removeMappingInternal(user: String, domain: String, mapping: String) {
    throw new RecipientRewriteTableException("Read-Only implementation")
  }

  def getUserDomainMappingsInternal(user: String, domain: String) =
    mappings.get(user + "@" + domain)
      .map(v => RecipientRewriteTableUtil.mappingToCollection(v))
      .orNull

  def getAllMappingsInternal = mappings.map(t => (t._1,  RecipientRewriteTableUtil.mappingToCollection(t._2)))

  def mapAddressInternal(user: String, domain: String) = RecipientRewriteTableUtil.getTargetString(user, domain, mappings)
}
