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

import org.scalatest.fixture.FunSuite
import org.eknet.publet.james.data.MailDb
import org.eknet.scue.{OrientDbFactory, DbFactory}
import org.eknet.publet.ext.graphdb.{BlueprintGraph, GraphDb}
import com.tinkerpop.blueprints.{Element, Edge, Vertex}
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion
import com.tinkerpop.blueprints.impls.orient.OrientGraph

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.11.12 23:04
 */
trait MaildbFixture extends FunSuite {

  type FixtureParam = MailDb

  def withFixture(test: OneArgTest) {
    withMailDb(db => withFixture(test.toNoArgTest(db)))
  }

  def withMailDb[A](f: MailDb => Any) {
    DbFactory.withDb(new OrientDbFactory())(db => {
      val maildb = new MailDb(new GraphDb(new OrientGraphWrapper(db)), new NullUsersRepository)
      f(maildb)
    })
  }

}

class OrientGraphWrapper(graph: OrientGraph) extends BlueprintGraph {
  def getFeatures = graph.getFeatures
  def addVertex(id: Any) = graph.addVertex(id)
  def getVertex(id: Any) = graph.getVertex(id)
  def removeVertex(vertex: Vertex) { graph.removeVertex(vertex) }
  def getVertices = graph.getVertices
  def getVertices(key: String, value: Any) = graph.getVertices(key, value)
  def addEdge(id: Any, outVertex: Vertex, inVertex: Vertex, label: String) = graph.addEdge(id, outVertex, inVertex, label)
  def getEdge(id: Any) = graph.getEdge(id)
  def removeEdge(edge: Edge) { graph.removeEdge(edge) }
  def getEdges = graph.getEdges
  def getEdges(key: String, value: Any) = graph.getEdges(key, value)
  def dropKeyIndex[T <: Element](key: String, elementClass: Class[T]) { graph.dropKeyIndex(key, elementClass) }
  def createKeyIndex[T <: Element](key: String, elementClass: Class[T]) { graph.createKeyIndex(key, elementClass) }
  def getIndexedKeys[T <: Element](elementClass: Class[T]) = graph.getIndexedKeys(elementClass)
  def stopTransaction(conclusion: Conclusion) { graph.stopTransaction(conclusion) }
  def shutdown() { graph.shutdown() }
}
