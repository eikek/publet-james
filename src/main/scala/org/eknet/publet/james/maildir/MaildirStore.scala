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
import org.apache.james.mailbox.model.MailboxPath
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

  def getMaildir(path: MailboxPath) = {
    getInbox(path.getUser).resolve(path.getName)
  }

  def getMaildir(mailbox: Mailbox[Int]) = {
    getInbox(mailbox.getUser).resolve(mailbox.getName)
  }

  def newUidProvider: UidProvider[Int] = new UidProviderImpl
  def newModSeqProvider: ModSeqProvider[Int] = new ModSeqProviderImpl

  private class UidProviderImpl extends UidProvider[Int] {
    def nextUid(session: MailboxSession, mailbox: Mailbox[Int]) = {
      val maildir = getMaildir(mailbox)
      maildir.uidlist.getNextUid
    }

    def lastUid(session: MailboxSession, mailbox: Mailbox[Int]) = {
      nextUid(session, mailbox) -1
    }
  }

  private class ModSeqProviderImpl extends ModSeqProvider[Int] {
    def nextModSeq(session: MailboxSession, mailbox: Mailbox[Int]) = {
      System.currentTimeMillis()
    }

    def highestModSeq(session: MailboxSession, mailbox: Mailbox[Int]) = {
      val maildir = getMaildir(mailbox)
      maildir.lastModified
    }
  }
}
