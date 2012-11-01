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

import org.eknet.publet.ext.orient.GraphDb
import org.eknet.scue.GraphDsl
import com.google.inject.{Inject, Singleton}
import org.apache.james.rrt.api.RecipientRewriteTable

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 28.10.12 20:00
 */
@Singleton
class MailDb @Inject() (db: GraphDb) {

  private implicit val graph = db.graph
  import GraphDsl._

  lazy val domains = vertex("name" := "domainNames", v => db.referenceNode --> "domains" -->| v)
  lazy val virtualAddr =  vertex("name" := "virtualAddress", v => db.referenceNode --> "virtualAddresses" -->| v)
  lazy val mappings = vertex("name" := "allMappings", v => db.referenceNode --> "mappings" -->| v)

  private val domainNameProp = "domainName"
  private val domainNameLabel = "domain"

  def addDomain(domain: String) {
    withTx {
      vertex(domainNameProp := domain.toLowerCase, v => domains --> domainNameLabel --> v)
    }
  }

  def removeDomain(domain: String) {
    withTx {
      vertices(domainNameProp := domain) foreach (dv => graph.removeVertex(dv))
    }
  }

  def containsDomain(domain: String): Boolean = db.withTx {
    vertices(domainNameProp := domain).headOption.isDefined
  }

  def getDomainList: List[String] = db.withTx {
    (domains ->- domainNameLabel mapEnds (v => v(domainNameProp).map(_.toString))).flatten.toList
  }

  /*
     Mapping outline
   */

  private val mappingLabel = "mapping"
  private val mappingProp = "mappingName"
  private val vaddressProp = "virtualAddress"
  private val addressLabel = "address"

  private def key(user: String, domain: String) =
    Option(user).getOrElse(RecipientRewriteTable.WILDCARD) +
    "@" + Option(domain).getOrElse(RecipientRewriteTable.WILDCARD)

  def addMapping(user: String, domain: String, mapping: String) {
    withTx {
      vertex(vaddressProp := key(user, domain), v => {
        virtualAddr --> addressLabel --> v
      }) --> mappingLabel -->| newVertex(mappingProp := mapping) <-- mappingLabel <-- mappings
    }
  }

  def removeMapping(user: String, domain: String, mapping: String) {
    db.withTx {
      vertices(vaddressProp := key(user, domain)).headOption.map(v => {
        (v ->- mappingLabel findEnd(_.has(mappingProp := mapping))).foreach(graph.removeVertex(_))
      })
    }
    withTx {
      if (userDomainMappings(user, domain) == Nil) {
        vertices(vaddressProp := key(user, domain)).headOption.map(v => graph.removeVertex(v))
      }
    }
  }

  def userDomainMappings(user: String, domain: String) = {
    vertices(vaddressProp := key(user, domain)).headOption.map(v => {
      (v ->- mappingLabel mapEnds(mn => mn(mappingProp).map(_.toString))).flatten
    }).getOrElse(Nil)
  }

  def allMappings = (virtualAddr ->- addressLabel mapEnds {van => (van(vaddressProp).toString ->
    (van ->- mappingLabel mapEnds(_(mappingProp).toString)).toList) }).toMap
}
