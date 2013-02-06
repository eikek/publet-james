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
import java.util.regex.Pattern
import grizzled.slf4j.Logging

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.01.13 00:28
 */
class MaildirMailboxMapper(session: MailboxSession, store: MaildirStore) extends MailboxMapper[Int] with NoTransaction with Logging {

  private val mailboxCache: collection.mutable.Map[Int, Mailbox[Int]] = new collection.mutable.HashMap()
  private val idCounter = new AtomicInteger(0)

  private def cacheMailbox(mbox: Mailbox[Int]) {
    val copy = new SimpleMailbox[Int](mbox)
    copy.setMailboxId(idCounter.getAndIncrement)
    debug("Cache mailbox " + copy.getMailboxId+":"+ copy.getName)
    mbox match {
      case smbox: SimpleMailbox[_] => smbox.asInstanceOf[SimpleMailbox[Int]].setMailboxId(copy.getMailboxId)
      case _ =>
    }
    mailboxCache.put(copy.getMailboxId, copy)
  }

  def save(mailbox: Mailbox[Int]) {
    debug("save mailbox "+ mailbox.getMailboxId+":"+mailbox.getName)
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
    debug("Delete mailbox "+ mailbox.getMailboxId+":"+mailbox.getName)
    val maildir = store.getMaildir(mailbox)
    maildir.delete()
  }

  def findMailboxByPath(path: MailboxPath) = {
    debug("Find mailbox by path: "+ path.toString)
    val maildir = store.getMaildir(path)
    if (!maildir.exists) {
      throw new MailboxNotFoundException(path)
    }
    val mbox = new MaildirMailbox[Int](maildir, path)
    cacheMailbox(mbox)
    mbox
  }

  def findMailboxWithPathLike(path: MailboxPath) = {
    debug("Find mailbox with pathlike: "+ path)
    import collection.JavaConversions._
    val inbox = store.getInbox(path.getUser)
    if (!inbox.exists) {
      List()
    } else {
      val pattern = Pattern.compile(Pattern.quote(path.getName).replace("%", ".*"))
      (inbox :: inbox.listChildren(includeSubfolder = true).toList)
        .withFilter(md => pattern.matcher(md.name).matches())
        .map(md => new MaildirMailbox[Int](md, new MailboxPath(path.getNamespace, path.getUser, md.name)))
        .toList
    }
  }

  def hasChildren(mailbox: Mailbox[Int], delimiter: Char) = {
    val maildir = store.getMaildir(mailbox)
    if (!maildir.exists) {
      throw new MailboxNotFoundException(mailbox.getName)
    }
    debug("mailbox "+ mailbox.getMailboxId+":"+mailbox.getName + " has children: "+ maildir.hasChildren)
    maildir.hasChildren
  }

  def list() = {
    debug("List mailboxes for "+ session.getUser.getUserName)
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