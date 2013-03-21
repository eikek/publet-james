/*
 * Copyright 2013 Eike Kettner
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

package org.eknet.publet.james.stats

import org.eknet.publet.vfs.Path
import annotation.tailrec
import collection.mutable
import org.eknet.publet.james.stats.CounterTree.{Leaf, Inner, Node}
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.eknet.publet.Glob

/**
 * This is a tree for organizing counters. Real counters are only valid at
 * the leaf nodes and every inner node is just a view of all of its children.
 *
 * If an inner node counter is incremented, it increments all of its children.
 * This yields in incrementing all real counters of all leaf nodes that are
 * children to the inner node.
 *
 * Besides the inner node composite counters, you can create arbitary composite
 * counters by using `getCompositeCounter` or `searchCoutners`.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.03.13 17:36
 */
class CounterTree(interval: Long) {

  private val root = Inner("_root_")

  private val lock = new ReentrantReadWriteLock()

  private[this] def lockRead[A](body: => A) = {
    lock.readLock().lock()
    try {
      body
    } finally {
      lock.readLock().unlock()
    }
  }

  private[this] def lockWrite[A](body: => A) = {
    lock.writeLock().lock()
    try {
      body
    } finally {
      lock.writeLock().unlock()
    }
  }

  /**
   * Returns a counter for the given path. The counter is created
   * if it does not exist yet.
   *
   * @param path
   * @return
   */
  def getCounter(path: Path): Counter = {
    if (path.isRoot) {
      root
    } else {
      val c = lockRead( find(root, path) )
      c.map(_._2).getOrElse {
        lockWrite(getOrCreate(root, path))
      }
    }
  }

  /**
   * Finds a counter for the given path.
   *
   * @param path
   * @return
   */
  def findCounter(path: Path): Option[Counter] = lockRead {
    if (path.isRoot) Some(root) else find(root, path).map(_._2)
  }

  /**
   * Gets the counter for each given path and returns a composite
   * counter of all of them.
   *
   * @param path
   * @return
   */
  def getCompositeCounter(path: Path*): CompositeCounter = {
    new CompositeCounter() {
      val counters = path.map(p => getCounter(p))
    }
  }

  /**
   * Searches for counters using a list of search patterns. The search pattern
   * are paths that may contain wildcards `?`, `*` or `**` as described at
   * [[org.eknet.publet.Glob]] class.
   *
   * For example:
   * {{{
   *   searchCounters("logins/*/success", "logins/ip/192*/success")
   * }}}
   *
   * @param path
   * @return
   */
  def searchCoutners(path: String*): CompositeCounter = {
    val all = lockRead( path.flatMap(p => search(List(root), p.split("/").filter(!_.isEmpty).toList)) )
    new CompositeCounter {
      val counters = all
    }
  }

  /**
   * Removes a counter at the given path, if it exists.
   *
   * @param path
   * @return
   */
  def removeCounter(path: Path): Option[Counter] = lockWrite {
    if (path.isRoot) {
      throw new IllegalArgumentException("Cannot remove root counter")
    }
    find(root, path) map { ct =>
      ct._1.children -= ct._2
      ct._2
    }
  }

  /**
   * Looks for a node for the given path and returns all of its
   * children names.
   *
   * @param path
   * @return
   */
  def getChildren(path: Path): Seq[String] = {
    val node = lockRead(find(root, path).map(_._2))
    node match {
      case Some(inner: Inner) => inner.children.map(_.name).toSeq
      case _ => Seq[String]()
    }
  }

  @tailrec
  private[this] def getOrCreate(start: Node, path: Path): Counter = {
    if (start.isInstanceOf[Leaf] || path.isRoot) {
      throw new IllegalStateException("Cannot go beyond leaf nodes")
    }
    val node = start.asInstanceOf[Inner]
    path.segments match {
      case a::Nil => {
        val next = node.findChild(a) getOrElse {
          node.addChild(new Leaf(a, interval))
        }
        next
      }
      case a::as => {
        val next = node.findChild(a) getOrElse {
          node.addChild(Inner(a))
        }
        getOrCreate(next, path.strip)
      }
      case Nil => sys.error("Unreachable code")
    }
  }

  @tailrec
  private[this] def find(start: Node, path: Path): Option[(Inner, Node)] = {
    if (path.isRoot) {
      None
    } else {
      if (start.isInstanceOf[Leaf]) {
        throw new IllegalStateException("Cannot go beyond leaf nodes")
      }
      val node = start.asInstanceOf[Inner]
      path.segments match {
        case a::Nil => {
          node.findChild(a).map(n => (node, n))
        }
        case a::as => {
          node.findInner(a) match {
            case None => None
            case Some(x) => find(x, path.strip)
          }
        }
        case Nil => sys.error("Unreachable code")
      }
    }
  }

  private[this] def search(nodes:List[Node], path: List[String]): List[Node] = {
    def nextNodes(node: Node, name: String) = node match {
      case inner:Inner => {
        val glob = Glob(name)
        inner.children.filter(n => glob.matches(n.name)).toList
      }
      case _ => List()
    }

    if (path.isEmpty) {
      nodes
    } else {
      nodes flatMap { node =>
        search(nextNodes(node, path.head), path.tail)
      }
    }
  }
}

object CounterTree {

  private[stats] sealed trait Node extends Counter {
    def name: String
  }

  private final case class Inner(name: String, children: mutable.ListBuffer[Node] = new mutable.ListBuffer[Node]()) extends Node with CompositeCounter {
    val counters = children

    def findChild(name: String) = children.find(_.name == name)
    def findLeaf(name: String) = children.collect({ case n: Leaf if (n.name == name) => n}).headOption
    def findInner(name: String) = children.collect({ case n: Inner if (n.name == name) => n}).headOption
    def addChild(node: Node) = {
      this.children += node
      node
    }
  }

  private final case class Leaf(name: String, interval: Long) extends BasicCounter(interval) with Node

}