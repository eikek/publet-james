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

package org.eknet.publet.james.maildir.lib

import org.scalatest.{BeforeAndAfterEach, BeforeAndAfter, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import java.nio.file.{Files, Paths}
import java.util.UUID

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 07.02.13 15:10
 */
class MaildirSuite extends FunSuite with ShouldMatchers with BeforeAndAfterEach {

  val inbox = new Maildir(Paths.get("target", "testmailbox", UUID.randomUUID().toString))

  override protected def withFixture(test: NoArgTest) {
    inbox.create()
    try {
      super.withFixture(test)
    } finally {
      inbox.folder.deleteTree()
    }
  }

  private def maildirCompare(d1: Maildir, d2: Maildir) = d1.name.compareTo(d2.name) < 0

  test ("list subfolder children deep") {
    inbox.resolve("subf2").create()
    inbox.resolve("subf3.subc1").create()
    inbox.resolve("subf3.subc2.subd1").create()
    inbox.resolve("subf3.subc2.subd2").create()
    inbox.resolve("subf3.subc2.subd3.sube1").create()
    inbox.resolve("subf3.subc2.subd3.sube2").create()
    val children = inbox.resolve("subf3.subc2")
      .listChildren(includeSubfolder = true)
      .toList.sortWith(maildirCompare)
    children should have size (5)
    children(0).name should be (".subf3.subc2.subd1")
    children(1).name should be (".subf3.subc2.subd2")
    children(2).name should be (".subf3.subc2.subd3")
    children(3).name should be (".subf3.subc2.subd3.sube1")
    children(4).name should be (".subf3.subc2.subd3.sube2")
  }

  test ("list inbox children deep") {
    inbox.resolve("subf1").create()
    inbox.resolve("subf2").create()
    inbox.resolve("subf3.subc1").create()
    inbox.resolve("subf3.subc2").create()
    inbox.resolve("subf3.subc3").create()
    val children = inbox
      .listChildren(includeSubfolder = true)
      .toList.sortWith(maildirCompare)
    children should have size (6)
    children(0).name should be (".subf1")
    children(1).name should be (".subf2")
    children(2).name should be (".subf3")
  }

  test ("list children of child") {
    inbox.resolve("subf1").create()
    inbox.resolve("subf2").create()
    inbox.resolve("subf3.subc1").create()
    inbox.resolve("subf3.subc2").create()
    inbox.resolve("subf3.subc3").create()
    val children = inbox.resolve("subf3")
      .listChildren(includeSubfolder = false)
      .toList.sortWith(maildirCompare)
    children should have size (3)
    children(0).name should be (".subf3.subc1")
    children(1).name should be (".subf3.subc2")
    children(2).name should be (".subf3.subc3")
  }

  test ("list child maildirs") {
    inbox.resolve("subf1").create()
    inbox.resolve("subf3").create()
    inbox.resolve("subf2").create()
    inbox.resolve("subf4.subf1").create()
    val children = inbox.listChildren(includeSubfolder = false)
      .toList
      .sortWith(maildirCompare)
    children should have size (4)
    children(0).name should be (".subf1")
    children(1).name should be (".subf2")
    children(2).name should be (".subf3")
    children(0).exists should be (true)
    children(1).exists should be (true)
    children(2).exists should be (true)
    children(3).name should be (".subf4")
    children(3).exists should be (false)
  }

  test ("create and delete nested subfolders") {
    val subf = inbox.resolve("subfolder1.subfolder2")
    subf.create()
    subf.exists should be (true)
    val path = subf.folder.subpath(inbox.folder.getNameCount, subf.folder.getNameCount)
    path.getNameCount should be (1)
    path should be (Paths.get(".subfolder1.subfolder2"))
    subf.delete()
    subf.exists should be (false)
    Files.exists(subf.folder) should be (false)
  }

  test ("create and delete subfolder maildir") {
    val subf = inbox.resolve("subfolder1")
    subf.create()
    subf.exists should be (true)
    subf.delete()
    subf.exists should be (false)
    Files.exists(subf.folder) should be (false)
  }

  test ("create root maildir") {
    val maildir = new Maildir(Paths.get(UUID.randomUUID().toString))
    maildir.exists should be (false)
    maildir.create()
    maildir.exists should be (true)
    maildir.folder.deleteTree()
  }

  test ("delete root maildir") {
    val maildir = new Maildir(Paths.get(UUID.randomUUID().toString))
    maildir.create()
    maildir.exists should be (true)
    maildir.delete()
    maildir.exists should be (false)
    Files.exists(maildir.folder) should be (true)

    maildir.create()
    maildir.exists should be (true)

    maildir.delete()
    maildir.exists should be (false)
    Files.exists(maildir.folder) should be (true)
    maildir.folder.deleteTree()
  }
}
