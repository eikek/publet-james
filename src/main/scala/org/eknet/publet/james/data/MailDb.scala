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

import org.eknet.publet.ext.orient.OrientDb
import com.google.inject.{Inject, Singleton}
import org.apache.james.domainlist.api.DomainList
import com.tinkerpop.blueprints.{Direction, Vertex}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 28.10.12 20:00
 */
@Singleton
class MailDb @Inject() (maildb: OrientDb) {

  import maildb._
  import collection.JavaConversions._

  lazy val domains = createSingle("name", "domainNames", "domains")
  lazy val users = createSingle("name", "userNames", "users")
  lazy val mappings = createSingle("name", "allMappings", "mappings")

  private def createSingle(key: String, value: String, label: String) = withTx {
    val iter = graph.findVertex(key, value)
    iter match {
      case Some(v) => v
      case None => {
        graph.createUniqueKeyIndex(key, classOf[Vertex])
        val v = addReferenceVertex(label)
        v.setProperty(key, value)
        v
      }
    }
  }

  private def createSingle(other: Vertex, direction: Direction, key: String, value: String, label: String) = withTx {
    val iter = other.getVertices(direction)
    iter.find(v => v.getProperty(key) == value) match {
      case Some(v) => v
      case None => {
        val v = graph.addVertex()
        v.setProperty(key, value)
        if (direction == Direction.OUT)
          graph.addEdge(null, other, v, label)
        else
          graph.addEdge(null, v, other, label)

        v
      }
    }
  }

//  graph.createUniqueKeyIndex("domainName", classOf[Vertex])

  private val domainNameProp = "domainName"
  private val domainNameLabel = "domain"

  def addDomain(domain: String) {
    createSingle(domains, Direction.OUT, domainNameProp, domain, domainNameLabel)
  }

  def removeDomain(domain: String) {
    withTx {
      val node = domains.getVertices(Direction.OUT, domainNameLabel)
        .find(v => v.getProperty(domainNameProp) == domain)

      node.foreach(n => graph.removeVertex(n))
    }
  }

  def containsDomain(domain: String): Boolean = withTx {
    domains.getVertices(Direction.OUT, domainNameLabel)
      .find(v => v.getProperty(domainNameProp) == domain)
      .isDefined
  }

  def getDomainList: List[String] = withTx {
    domains.getVertices(Direction.OUT, domainNameLabel)
      .map(v => v.getProperty(domainNameProp).toString)
      .toList
  }

  /*
     Mapping outline

     ref --[users]--> users --[<domain>]--> mapping  <---- allMappings
                      "john"  --[example.org] --> john@* <--- mappings
   */

  private val mappingLabel = "mapping"
  private val mappingProp = "mappingName"
  private val usernameProp = "username"
  private val usernameLabel = "user"


  def addMapping(user: String, domain: String, mapping: String) {
    withTx {
      val un = createSingle(users, Direction.OUT, usernameProp, user, usernameLabel)
      val mn = createSingle(un, Direction.OUT, mappingProp, mapping, domain)
      graph.addEdge(null, mappings, mn, mappingLabel)
    }
  }

  def removeMapping(user: String, domain: String, mapping: String) {
    withTx {
      mappings.getVertices(Direction.OUT, mappingLabel).foreach(mn => {
        mn.getEdges(Direction.IN).foreach( edge => {
          graph.removeVertex(edge.getVertex(Direction.IN))
          graph.removeEdge(edge)
          graph.removeVertex(mn)
        })
      })
    }
  }

  def getAllMappings: Map[String, String] = withTx {
    val map = collection.mutable.Map[String, String]()
    mappings.getVertices(Direction.OUT, mappingLabel).foreach(mn => {
      mn.getEdges(Direction.IN).foreach( edge => {
        val username = edge.getVertex(Direction.OUT)
        val domain = edge.getLabel
        val mapping = mn.getProperty(mappingProp)
        map.put(username+"@"+domain, mapping.toString)
      })
    })

    map.toMap
  }

}
