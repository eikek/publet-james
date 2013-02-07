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

import lib.{TextFileUidDb, Options, Maildir, PathLock}
import java.nio.file.{Paths, Path}
import org.apache.james.mailbox.model.{MailboxConstants, MailboxPath}
import java.net.URI
import java.util.Locale
import org.apache.james.mailbox.store.mail.model.Mailbox
import org.apache.james.mailbox.store.mail.{ModSeqProvider, UidProvider}
import org.apache.james.mailbox.MailboxSession
import com.google.inject.Singleton
import javax.inject.Inject

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.01.13 15:52
 */
class MaildirStore(maildirLocation: String, lock: PathLock[Path]) {

  private val maildirOptions = Options(
    uiddbProvider = TextFileUidDb.newProvider("publet-james-uidlist",
    lock = lock,
    maxValue = Int.MaxValue)
  )

  private def inboxPath(user: String) = {
    val ml = maildirLocation.replace("%fulluser", user)
    val parts = user.split("@", 2)
    if (parts.length == 2) {
      ml.replace("%domain", parts(1).toLowerCase(Locale.ROOT)).replace("%user", parts(0))
    } else {
      ml.replace("%user", user)
    }
  }

  def getInbox(user: String) = {
    val path = inboxPath(user) match {
      case p if (p.indexOf(':')> 0) => p
      case p => "file:"+ p
    }
    new Maildir(Paths.get(new URI(path)), maildirOptions)
  }

  private def getMaildir(user: String, name: String) = {
    if (name == MailboxConstants.INBOX) {
      getInbox(user)
    }
    else {
      getInbox(user).resolve(stripInbox(name))
    }
  }

  def getMaildir(path: MailboxPath): Maildir = getMaildir(path.getUser, path.getName)

  def getMaildir(mailbox: Mailbox[Int]): Maildir = getMaildir(mailbox.getUser, mailbox.getName)

  def getMaildir(mm: MaildirMessage): Maildir = {
    val folder = mm.file.getParent
    new Maildir(folder, maildirOptions)
  }

  def nextUid(mailbox: Mailbox[Int]) = getMaildir(mailbox).uidlist.getNextUid
  def lastUid(mailbox: Mailbox[Int]) = nextUid(mailbox) -1

  def nextModSeq = System.currentTimeMillis()
  def highestModSeq(mailbox: Mailbox[Int]) = getMaildir(mailbox).lastModified
}
