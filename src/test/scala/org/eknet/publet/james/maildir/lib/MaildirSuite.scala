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

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import java.nio.file.{Files, Paths}
import java.util.UUID
import org.eknet.publet.james.maildir.MessageProvider
import javax.mail.Flags

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

  test ("set flags") {
    import MessageProvider._
    val added1 = inbox.putMessage(new MyMessageFile(readMessage(testmailLfLf), recent = false))
    val flags = new Flags()
    flags.add(Flags.Flag.RECENT)
    val added2 = inbox.setFlags(added1, flags)
    inbox.isRecent(added2.name) should be (true)
    inbox.getMessage(added1.uid).name should be (added2.name)

    flags.remove(Flags.Flag.RECENT)
    flags.add(Flags.Flag.SEEN)
    flags.add(Flags.Flag.ANSWERED)
    val added3 = inbox.setFlags(added2, flags)
    inbox.isCurrent(added3.name) should be (true)
    // "Flags must be stored in ASCII order: e.g., '2,FRS'": http://cr.yp.to/proto/maildir.html
    added3.name.flags should be (List("R", "S"))
  }

  test ("put and delete message") {
    import MessageProvider._
    val added1 = inbox.putMessage(new MyMessageFile(readMessage(testmailLfLf), recent = false))
    inbox.isCurrent(added1.name) should be (true)
    inbox.isCurrent(added1.uid) should be (true)
    inbox.isRecent(added1.name) should be (false)
    inbox.isRecent(added1.uid) should be (false)
    added1.file.exists should be (true)
    added1.file.isFile should be (true)
    inbox.findMessage(added1.uid) should be (Some(added1))

    val added2 = inbox.putMessage(new MyMessageFile(readMessage(testmailLfLf), recent = true))
    inbox.isCurrent(added2.name) should be (false)
    inbox.isCurrent(added2.uid) should be (false)
    inbox.isRecent(added2.name) should be (true)
    inbox.isRecent(added2.uid) should be (true)
    added2.file.exists should be (true)
    added2.file.isFile should be (true)
    inbox.findMessage(added2.uid) should be (Some(added2))

    inbox.deleteMessage(added1.uid)
    added1.file.exists should be (false)
    inbox.findMessage(added1.uid) should be (None)

    inbox.deleteMessage(added2.uid)
    added2.file.exists should be (false)
    inbox.findMessage(added2.uid) should be (None)
  }

  test ("has children") {
    inbox.hasChildren should be (false)
    inbox.resolve("sub1/sub2").create()
    inbox.hasChildren should be (true)
    inbox.resolve("sub3").create()
    inbox.hasChildren should be (true)

    inbox.delete()
    inbox.create()
    inbox.resolve("sub1").create()
    inbox.resolve("sub2").create()
    inbox.hasChildren should be (true)
    inbox.resolve("sub2").hasChildren should be (false)
    inbox.resolve("sub4.sub1").create()
    inbox.resolve("sub4").hasChildren should be (true)
  }

  test ("resolve child") {
    val sub1 = inbox.resolve("sub1")
    sub1.name should be (".sub1")
    val sub1sub2 = sub1.resolve("sub2")
    sub1sub2.name should be (".sub1.sub2")
    sub1.rootMaildir.folder == sub1sub2.rootMaildir.folder

    val ab = inbox.resolve("a/b")
    ab.create()
    ab.name should be (".a.b")
  }

  test ("rename maildir") {
    val messages = inbox.getMessages(UidRange.All).size
    val folders = inbox.listChildren().size
    val mbox = inbox.rename("ymca")
    mbox.exists should be (true)
    inbox.exists should be (false)
    mbox.getMessages(UidRange.All) should have size (messages)
    mbox.listChildren() should have size (folders)
    mbox.rename(inbox.folder.getFileName.toString)
  }

  test ("move message") {
    import MessageProvider._
    val added = inbox.putMessage(new MyMessageFile(readMessage(testmailLfLf)))
    inbox.getMessages(UidRange.All) should have size (1)
    val targetBox = new Maildir(Paths.get("target", "testmailbox", UUID.randomUUID().toString))
    targetBox.create()
    try {
      val moved = inbox.moveMessage(added.uid, targetBox)
      inbox.getMessages(UidRange.All) should have size (0)
      targetBox.getMessages(UidRange.All) should have size (1)
      moved.name should be (added.name)
    } finally {
      targetBox.folder.deleteTree()
    }
  }

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

  test ("Maildir names with whitespace") {
    val maildir = new Maildir(Paths.get(UUID.randomUUID().toString))
    maildir.create()
    val folder = maildir.folder
    val subdir1 = folder.resolve(".Testbox1")
    val subdir2 = folder.resolve(".Test box2")
    val subdir3 = folder.resolve(".INBOX.Testbox3")
    val subdir4 = folder.resolve(".INBOX.Test box 4")
    List(subdir1, subdir2, subdir3, subdir4).foreach(_.createDirectories)
    List(subdir1, subdir2, subdir3, subdir4).foreach(d => new Maildir(d).create())

    maildir.listChildren(true).toList should have size (5)
    maildir.listChildren(false).toList should have size (3)
    folder.deleteTree()
  }
}
