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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.eknet.publet.vfs.Path

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.03.13 18:22
 */
class CounterSuite extends FunSuite with ShouldMatchers {
  import org.eknet.publet.vfs.Path.stringToPath

  test ("adding counters") {
    val tree = new CounterTree(1000)

    val n = tree.getCounter(Path.root)
    n.toString should be ("Inner(_root_,ListBuffer())")

    val b = tree.getCounter("logins/imap/john/success".p)
    b.increment()
    b.increment()
    b.totalCount should be (2)
    b.intervalCount should be (2)

    tree.getCounter("logins/imap/john/failed".p).increment()
    tree.getCounter("logins/imap/john/failed".p).increment()

    var tmp = tree.getCounter("logins/imap/john".p)
    tmp.totalCount should be (4)

    tree.getCounter("blacklist/smtp".p).increment()
    tree.getCounter("blacklist/smtp".p).increment()
    tree.getCounter("blacklist/smtp".p).increment()

    tmp = tree.getCompositeCounter("logins/imap/john".p, "blacklist/smtp".p)
    tmp.totalCount should be (7)
  }

  test ("using interval counter") {
    val c = new BasicCounter(500)

    c.increment()
    c.increment()
    Thread.sleep(100)
    c.increment()
    c.intervalCount should be (3)

    c.increment()
    c.intervalCount should be (4)

    Thread.sleep(600)
    c.increment()
    c.intervalCount should be (1)
    c.totalCount should be (5)
  }

  test ("remove counter") {
    val tree = new CounterTree(1000)
    tree.getCompositeCounter("/a/b/c".p, "/a/d/e".p, "/z/d/f".p).increment()
    tree.getCompositeCounter("/a/b/c".p, "/a/d/e".p, "/z/d/f".p).increment()
    tree.getCompositeCounter("/a/b/c".p, "/a/d/e".p, "/z/d/f".p).increment()

    val rc = tree.getCounter(Path.root)
    rc.totalCount should be (9)

    val removed = tree.removeCounter("/a/d/e".p)
    removed should not be (None)

    rc.totalCount should be (6)

    tree.removeCounter("/xy/t".p) should be (None)
  }

  test ("search counters with globs") {
    val tree = new CounterTree(1000)

    tree.getCounter("/a/b1/c/x".p).add(1)
    tree.getCounter("/a/b2/c/x".p).add(2)
    tree.getCounter("/a/b3/c/x".p).add(3)

    var cc = tree.searchCoutners("/a/*/c")
    cc.counters should have size (3)
    cc.totalCount should be (6)

    cc = tree.searchCoutners("a/b?/c/x")
    cc.counters should have size (3)
    cc.totalCount should be (6)

    tree.getCounter("/b/b1/c/x".p).add(5)
    tree.getCounter("/c/b1/c/x".p).add(5)
    tree.getCounter("/d/b1/c/x".p).add(5)
    cc = tree.searchCoutners("*/b1/c")
    cc.totalCount should be (16)
    cc.counters should have size (4)
  }
}
