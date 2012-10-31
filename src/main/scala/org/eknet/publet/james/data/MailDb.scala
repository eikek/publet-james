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

  lazy val domains = db.referenceNode --> "domains" -->| vertex("name", "domainNames")
  lazy val virtualAddr =  db.referenceNode --> "virtualAddresses" -->| vertex("name", "virtualAddress")
  lazy val mappings = db.referenceNode --> "mappings" -->| vertex("name", "allMappings")

  private val domainNameProp = "domainName"
  private val domainNameLabel = "domain"

  def addDomain(domain: String) {
    withTx {
      domains --> domainNameLabel --> vertex(domainNameProp, domain)
    }
  }

  def removeDomain(domain: String) {
    withTx {
      (domains ->- domainNameLabel findEnd(v => v(domainNameProp) == domain))
        .map { v => graph.removeVertex(v) }
    }
  }

  def containsDomain(domain: String): Boolean = db.withTx {
    (domains ->- domainNameLabel findEnd(v => v(domainNameProp) == domain)).isDefined
  }

  def getDomainList: List[String] = db.withTx {
    (domains ->- domainNameLabel mapEnds (v => v(domainNameProp).toString)).toList
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
    db.withTx {
      virtualAddr --> addressLabel -->|
        vertex(vaddressProp, key(user, domain)) --> mappingLabel -->|
        newVertex(v => v(mappingProp) = mapping) <-- mappingLabel <-- mappings
    }
  }

  def removeMapping(user: String, domain: String, mapping: String) {
    db.withTx {
      virtualAddr ->- addressLabel findEnd(v => v(vaddressProp) == key(user, domain)) map {
        _ ->- mappingLabel findEnd(v => v(mappingProp) == mapping) map (graph.removeVertex(_))
      }
    }
  }

  def userDomainMappings(user: String, domain: String) = {
    virtualAddr ->- addressLabel findEnd(_(mappingProp) == key(user, domain)) map {
      _ ->- mappingLabel mapEnds(mn => mn(mappingProp).toString)
    } getOrElse(Nil)
  }

  def allMappings = (virtualAddr ->- addressLabel mapEnds {van => (van(vaddressProp).toString ->
    (van ->- mappingLabel mapEnds(_(mappingProp).toString)).toList) }).toMap
}
