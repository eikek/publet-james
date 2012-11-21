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
import org.eknet.scue.{TitanDbFactory, DbFactory}
import org.eknet.publet.ext.graphdb.{BlueprintGraph, GraphDb}
import com.thinkaurelius.titan.core.TitanGraph
import com.tinkerpop.blueprints.{Element, Edge, Vertex}
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.11.12 23:04
 */
trait MaildbFixture extends FunSuite {

  type FixtureParam = MailDb

  def withFixture(test: OneArgTest) {
    DbFactory.withDb(new TitanDbFactory())(db => {
      val maildb = new MailDb(new GraphDb(new TitanWrapper(db)))
      test.toNoArgTest(maildb)
    })
  }

  private class TitanWrapper(titan: TitanGraph) extends BlueprintGraph {
    def getFeatures = titan.getFeatures
    def addVertex(id: Any) = titan.addVertex(id)
    def getVertex(id: Any) = titan.getVertex(id)
    def removeVertex(vertex: Vertex) { titan.removeVertex(vertex) }
    def getVertices = titan.getVertices
    def getVertices(key: String, value: Any) = titan.getVertices(key, value)
    def addEdge(id: Any, outVertex: Vertex, inVertex: Vertex, label: String) = titan.addEdge(id, outVertex, inVertex, label)
    def getEdge(id: Any) = titan.getEdge(id)
    def removeEdge(edge: Edge) { titan.removeEdge(edge) }
    def getEdges = titan.getEdges
    def getEdges(key: String, value: Any) = titan.getEdges(key, value)
    def dropKeyIndex[T <: Element](key: String, elementClass: Class[T]) { titan.dropKeyIndex(key, elementClass) }
    def createKeyIndex[T <: Element](key: String, elementClass: Class[T]) { titan.createKeyIndex(key, elementClass) }
    def getIndexedKeys[T <: Element](elementClass: Class[T]) = titan.getIndexedKeys(elementClass)
    def stopTransaction(conclusion: Conclusion) { titan.stopTransaction(conclusion) }
    def shutdown() { titan.shutdown() }
  }
}
