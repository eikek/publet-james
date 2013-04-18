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

package org.eknet.publet.james.maildir

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.util.{Locale, UUID}
import org.apache.james.mailbox.store.SimpleMailboxSession
import org.slf4j.LoggerFactory
import org.apache.james.mailbox.MailboxSession.SessionType
import java.io.File
import org.apache.james.mailbox.model.MailboxPath

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 18.04.13 22:07
 */
class MaildirMailboxMapperSuite extends FunSuite with ShouldMatchers {
  import collection.JavaConversions._
  import lib._

  val logger = LoggerFactory.getLogger(getClass)
  val mstore = new MaildirStore(new File(UUID.randomUUID().toString).getAbsolutePath, new JvmLocker())
  val msession = new SimpleMailboxSession(1L, "admin", "admin", logger, List(Locale.ENGLISH), '/', SessionType.User)
  val mm = new MaildirMailboxMapper(msession, mstore)

  test ("find mailboxes") {
    val maildir = mstore.getInbox("admin")
    maildir.create()
    val folder = maildir.folder
    val subdir1 = folder.resolve(".Testbox1")
    val subdir2 = folder.resolve(".Test box2")
    val subdir3 = folder.resolve(".INBOX.Testbox3")
    val subdir4 = folder.resolve(".INBOX.Test box 4")
    List(subdir1, subdir2, subdir3, subdir4).foreach(_.createDirectories)
    List(subdir1, subdir2, subdir3, subdir4).foreach(d => new Maildir(d).create())

    val tb4p = new MailboxPath("#private", "admin", "INBOX.Test box 4")
    val tb4 = mm.findMailboxByPath(tb4p)
    tb4.maildir.exists should be (true)

    val tb2 = mm.findMailboxByPath(new MailboxPath("#private", "admin", "Test box2"))
    tb2.maildir.exists should be (true)
  }
}
