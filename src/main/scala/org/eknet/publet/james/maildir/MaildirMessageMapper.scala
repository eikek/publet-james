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

import lib.{UidRange, MyMessage}
import org.apache.james.mailbox.store.mail.{MessageMapper, AbstractMessageMapper}
import org.apache.james.mailbox.{model, MailboxSession}
import org.apache.james.mailbox.store.mail.model.{Message, Mailbox}
import org.apache.james.mailbox.model.{UpdatedFlags, MessageRange}
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType
import java.io.{BufferedInputStream, InputStream, OutputStream}
import com.google.common.io.{ByteStreams, InputSupplier}
import org.apache.james.mailbox.store.SimpleMessageMetaData
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage
import javax.mail.Flags
import grizzled.slf4j.Logging
import java.nio.file.Files
import collection.SortedMap

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.01.13 19:20
 */
class MaildirMessageMapper(store: MaildirStore, session: MailboxSession) extends MessageMapper[Int] with NoTransaction with Logging {

  implicit private[this] def messageRangeToRange(range: MessageRange) = range.getType match {
    case MessageRange.Type.ALL => UidRange.All
    case MessageRange.Type.FROM => UidRange.From(range.getUidFrom)
    case MessageRange.Type.ONE => UidRange.Single(range.getUidFrom)
    case MessageRange.Type.RANGE => UidRange.Interval(range.getUidFrom, range.getUidTo)
  }

  def getLastUid(mailbox: Mailbox[Int]) = store.lastUid(mailbox)

  def getHighestModSeq(mailbox: Mailbox[Int]) = store.highestModSeq(mailbox)

  def findInMailbox(mailbox: Mailbox[Int], range: MessageRange, fetchType: FetchType, max: Int) = {
    debug("> findInMailbox: "+ mailbox.getMailboxId+":"+mailbox.getName+" = "+ messageRangeToRange(range)+ "; "+ fetchType+ "; max="+max)
    import collection.JavaConversions._
    val maildir = store.getMaildir(mailbox)
    maildir.getMessages(range).values
      .map(m => MaildirMessage.from(mailbox.getMailboxId, m))
      .toList
      .sortWith((m1, m2) => m1.uid.compareTo(m2.uid) < 0)
      .take(max)
      .iterator
  }

  def countMessagesInMailbox(mailbox: Mailbox[Int]) = {
    debug("> count messages in mailbox: "+ mailbox.getMailboxId+":"+mailbox.getName)
    val maildir = store.getMaildir(mailbox)
    maildir.getMessages(UidRange.All).size
  }

  def countUnseenMessagesInMailbox(mailbox: Mailbox[Int]) = {
    debug("> count unseen messages in mailbox: "+ mailbox.getMailboxId+":"+mailbox.getName)
    val maildir = store.getMaildir(mailbox)
    maildir.getMessages(UidRange.All)
      .filter(t => !t._2.name.flags.contains("S"))
      .size
  }

  def delete(mailbox: Mailbox[Int], message: Message[Int]) {
    debug("> delete messages in mailbox: "+ mailbox.getMailboxId+":"+mailbox.getName+" => "+ message.getUid)
    val maildir = store.getMaildir(mailbox)
    maildir.deleteMessage(message.getUid)
  }

  def findFirstUnseenMessageUid(mailbox: Mailbox[Int]) = {
    debug("> find first unseen message uid: " +mailbox.getMailboxId+":"+mailbox.getName)
    val maildir = store.getMaildir(mailbox)
    maildir.getMessages(UidRange.All).find(t => !t._2.name.flags.contains("S"))
      .map(t => Long.box(t._1))
      .orNull
  }

  def findRecentMessageUidsInMailbox(mailbox: Mailbox[Int]) = {
    debug("> find recent message uids in mailbox: "+ mailbox.getMailboxId+":"+mailbox.getName)
    import collection.JavaConversions._
    val maildir = store.getMaildir(mailbox)
    maildir.getMessages(UidRange.All).values
      .withFilter(m => maildir.isRecent(m.name))
      .map(m => Long.box(m.uid))
      .toList
      .sorted
  }

  def expungeMarkedForDeletionInMailbox(mailbox: Mailbox[Int], range: MessageRange) = {
    debug("> expunge deleted messages in mailbox: "+ mailbox.getMailboxId+":"+mailbox.getName+ "; range="+messageRangeToRange(range))
    import collection.JavaConversions._
    val maildir = store.getMaildir(mailbox)
    val marked = maildir.getMessages(range).values
      .withFilter(mf => mf.name.flags.contains("T"))
      .map { mf => MaildirMessage.from(mailbox.getMailboxId, mf) }

    val meta = marked.map(m => Long.box(m.uid) -> new SimpleMessageMetaData(m)).toSeq
    marked.foreach(m => maildir.deleteMessage(m.uid))
    SortedMap(meta: _*)
  }

  def move(mailbox: Mailbox[Int], message: Message[Int]) = {
    throw new UnsupportedOperationException("Not implemented - see https://issues.apache.org/jira/browse/IMAP-370")
  }

  def updateFlags(mailbox: Mailbox[Int], flags: Flags, value: Boolean, replace: Boolean, set: MessageRange) = {
    debug("> update flags: "+ mailbox.getMailboxId+":"+mailbox.getName+"; flags="+flagsToString(flags)+"; value="+value+"; replace="+replace+"; set="+set)
    import collection.JavaConversions._
    val maildir = store.getMaildir(mailbox)
    val iter = for (mf <- maildir.getMessages(set).values) yield {
      val orgFlags = mf.name.getFlags
      val newFlags = if (replace) { flags } else {
        val tmp = mf.name.getFlags
        if (value) tmp.add(flags) else tmp.remove(flags)
        tmp
      }
      if (orgFlags == newFlags) {
        new model.UpdatedFlags(mf.uid, Files.getLastModifiedTime(mf.file).toMillis, orgFlags, newFlags)
      } else {
        val modSeq = Files.getLastModifiedTime(maildir.setFlags(mf.uid, newFlags).file)
        new UpdatedFlags(mf.uid, modSeq.toMillis, orgFlags, newFlags)
      }
    }
    iter.iterator
  }

  def add(mailbox: Mailbox[Int], message: Message[Int]) = {
    debug("> save message in mailbox: "+ mailbox.getMailboxId+":"+mailbox.getName+" => "+ message.getUid)
    val maildir = store.getMaildir(mailbox)
    val mfile = maildir.putMessage(new MyMessageMessage(message))
    message.setUid(mfile.uid)
    message.setModSeq(maildir.lastModified)
    new SimpleMessageMetaData(message)
  }

  def copy(mailbox: Mailbox[Int], message: Message[Int]) = {
    val copyMsg = new SimpleMessage[Int](mailbox, message)
    val flags = copyMsg.createFlags()
    flags.add(Flags.Flag.RECENT)
    copyMsg.setFlags(flags)
    add(mailbox, copyMsg)
  }

  // unused methods
  def begin() {}
  def commit() {}
  def rollback() {}
  def endRequest() {}

  private class MyMessageMessage(msg: Message[Int]) extends MyMessage {

    def isRecent = msg.isRecent

    def getSize = Some(msg.getFullContentOctets.toInt)

    def getFlags = msg.createFlags()

    def writeTo(out: OutputStream) {
      val inputs = new InputSupplier[InputStream] {
        def getInput = new BufferedInputStream(msg.getFullContent)
      }
      ByteStreams.copy(inputs, out)
      out.close()
    }
  }
}
