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

import org.eknet.publet.ext.orient.{GraphDb, GraphDsl}
import com.google.inject.{Inject, Singleton}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 28.10.12 20:00
 */
@Singleton
class MailDb @Inject() (maildb: GraphDb) {

  private implicit val graph = maildb.graph
  import maildb._
  import GraphDsl._

  lazy val domains = referenceNode --> "domains" -->| vertex("name", "domainNames")
  lazy val users =  referenceNode --> "users" -->| vertex("name", "userNames")
  lazy val mappings = referenceNode --> "mappings" -->| vertex("name", "allMappings")

  private val domainNameProp = "domainName"
  private val domainNameLabel = "domain"

  def addDomain(domain: String) {
    domains --> domainNameLabel --> vertex(domainNameProp, domain)
  }

  def removeDomain(domain: String) {
    withTx {
      (domains ->- domainNameLabel findEnd(v => v(domainNameProp) == domain))
        .map { v => graph.removeVertex(v) }
    }
  }

  def containsDomain(domain: String): Boolean = withTx {
    (domains ->- domainNameLabel findEnd(v => v(domainNameProp) == domain)).isDefined
  }

  def getDomainList: List[String] = withTx {
    (domains ->- domainNameLabel mapEnds (v => v(domainNameProp).toString)).toList
  }

  /*
     Mapping outline

     ref --[users]--> users --[<domain>]--> mapping  <---- allMappings
                      "john"  --[example.org] --> john@* ---> mappings
   */

  private val mappingLabel = "mapping"
  private val mappingProp = "mappingName"
  private val usernameProp = "username"
  private val usernameLabel = "user"


  def addMapping(user: String, domain: String, mapping: String) {
    withTx {
      val un = users --> usernameLabel -->| vertex(usernameProp, user)
      val mn = un --> domain -->| vertex(mappingProp, mapping)
      mn --> mappingLabel --> mappings
    }
  }

  def removeMapping(user: String, domain: String, mapping: String) {
    withTx {
      (mappings -<- mappingLabel).foreachEnd(mn => {
        graph.removeVertex(mn)
      })
    }
  }

  def getAllMappings: Map[String, String] = withTx {
    val map = collection.mutable.Map[String, String]()
    (mappings -<- mappingLabel).foreachEnd(mappingVertex => {
      mappingVertex -<-() foreach( edge => {
        val username = edge.outVertex(usernameProp).toString
        val domain = edge.label
        val mapping = mappingVertex(mappingProp)
        map.put(username+"@"+domain, mapping.toString)
      })
    })

    map.toMap
  }

}
