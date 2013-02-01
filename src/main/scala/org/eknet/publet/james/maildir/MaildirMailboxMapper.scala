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

import org.apache.james.mailbox.store.mail.MailboxMapper
import org.apache.james.mailbox.store.transaction.Mapper.Transaction
import org.apache.james.mailbox.store.mail.model.Mailbox
import org.apache.james.mailbox.model.{MailboxACL, MailboxPath}
import org.apache.james.mailbox.MailboxSession
import org.apache.james.mailbox.exception.MailboxNotFoundException
import java.util.concurrent.atomic.AtomicInteger
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.01.13 00:28
 */
class MaildirMailboxMapper(session: MailboxSession, store: MaildirStore) extends MailboxMapper[Int] with NoTransaction {

  private val mailboxCache: collection.mutable.Map[Int, Mailbox[Int]] = new collection.mutable.HashMap()
  private val idCounter = new AtomicInteger(0)

  private def cacheMailbox(mbox: Mailbox[Int]) {
    val copy = new SimpleMailbox[Int](mbox)
    copy.setMailboxId(idCounter.getAndIncrement)
    mbox match {
      case smbox: SimpleMailbox[Int] => smbox.setMailboxId(copy.getMailboxId)
      case _ =>
    }
    mailboxCache.put(copy.getMailboxId, copy)
  }

  def save(mailbox: Mailbox[Int]) {
    mailboxCache.get(mailbox.getMailboxId) match {
      case Some(orgMbox) => {
        if (orgMbox.getName != mailbox.getName) {
          val orgDir = store.getMaildir(orgMbox)
          val target = store.getMaildir(mailbox)
          orgDir.rename(target.name)
        }
      }
      case None => {
        val target = store.getMaildir(mailbox)
        if (!target.exists) {
          target.create()
        }
        target.uidlist.setUidValidity(mailbox.getUidValidity)
      }
    }
  }

  def delete(mailbox: Mailbox[Int]) {
    val maildir = store.getMaildir(mailbox)
    maildir.delete()
  }

  def findMailboxByPath(path: MailboxPath) = {
    val maildir = store.getMaildir(path)
    if (!maildir.exists) {
      throw new MailboxNotFoundException(path)
    }
    val mbox = new MaildirMailbox[Int](maildir, path)
    cacheMailbox(mbox)
    mbox
  }

  def findMailboxWithPathLike(path: MailboxPath) = {
    throw new UnsupportedOperationException()
  }

  def hasChildren(mailbox: Mailbox[Int], delimiter: Char) = {
    val maildir = store.getMaildir(mailbox)
    if (!maildir.exists) {
      throw new MailboxNotFoundException(mailbox.getName)
    }
    maildir.hasChildren
  }

  def list() = {
    import collection.JavaConversions._
    val maildir = store.getInbox(session.getUser.getUserName)
    if (!maildir.exists) {
      throw new MailboxNotFoundException(MailboxPath.inbox(session))
    }
    maildir.listChildren(includeSubfolder = true)
      .map(mdir => new MaildirMailbox[Int](mdir, MailboxPath.parse(session, mdir.name)))
      .toList
  }

  def endRequest() {
    mailboxCache.clear()
  }

}