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
import org.apache.james.mailbox.store.mail.{AbstractMessageMapper, ModSeqProvider, UidProvider, MessageMapper}
import org.apache.james.mailbox.MailboxSession
import org.apache.james.mailbox.store.mail.model.{Message, Mailbox}
import org.apache.james.mailbox.model.MessageRange
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType
import java.io.{BufferedInputStream, InputStream, OutputStream}
import com.google.common.io.{ByteStreams, InputSupplier, Files}
import com.google.common.base.Suppliers
import org.apache.james.mailbox.store.SimpleMessageMetaData
import org.apache.james.mailbox.store.mail.model.impl.SimpleMessage
import javax.mail.Flags

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.01.13 19:20
 */
class MaildirMessageMapper(store: MaildirStore, session: MailboxSession)
  extends AbstractMessageMapper[Int](session, store.newUidProvider, store.newModSeqProvider) {

  implicit private[this] def messageRangeToRange(range: MessageRange) = range.getType match {
    case MessageRange.Type.ALL => UidRange.All
    case MessageRange.Type.FROM => UidRange.From(range.getUidFrom)
    case MessageRange.Type.ONE => UidRange.Single(range.getUidFrom)
    case MessageRange.Type.RANGE => UidRange.Interval(range.getUidFrom, range.getUidTo)
  }

  def findInMailbox(mailbox: Mailbox[Int], range: MessageRange, fetchType: FetchType, max: Int) = {
    import collection.JavaConversions._
    val maildir = store.getMaildir(mailbox)
    null
  }

  def countMessagesInMailbox(p1: Mailbox[Int]) = 0L

  def countUnseenMessagesInMailbox(p1: Mailbox[Int]) = 0L

  def delete(mailbox: Mailbox[Int], message: Message[Int]) {
    val maildir = store.getMaildir(mailbox)
    maildir.deleteMessage(message.getUid)
  }

  def findFirstUnseenMessageUid(p1: Mailbox[Int]) = null

  def findRecentMessageUidsInMailbox(p1: Mailbox[Int]) = null

  def expungeMarkedForDeletionInMailbox(mailbox: Mailbox[Int], range: MessageRange) = {
    val maildir = store.getMaildir(mailbox)
    maildir.getMessages(range).withFilter(_._2._1.flags.contains("D"))
    null
  }

  def move(p1: Mailbox[Int], p2: Message[Int]) = {
    throw new UnsupportedOperationException("Not implemented - see https://issues.apache.org/jira/browse/IMAP-370")
  }

  def save(mailbox: Mailbox[Int], message: Message[Int]) = {
    val maildir = store.getMaildir(mailbox)
    val uid = maildir.putMessage(new MyMessageMessage(message))
    message.setUid(uid)
    message.setModSeq(maildir.lastModified)
    new SimpleMessageMetaData(message)
  }

  def copy(mailbox: Mailbox[Int], uid: Long, modseq: Long, message: Message[Int]) = {
    val copyMsg = new SimpleMessage[Int](mailbox, message)
    val flags = copyMsg.createFlags()
    flags.add(Flags.Flag.RECENT)
    copyMsg.setFlags(flags)
    save(mailbox, copyMsg)
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
