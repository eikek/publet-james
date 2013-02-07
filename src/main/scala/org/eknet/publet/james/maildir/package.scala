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

package org.eknet.publet.james

import org.apache.james.mailbox.store.transaction.Mapper
import org.apache.james.mailbox.store.transaction.Mapper.Transaction
import org.apache.james.mailbox.store.mail.model.Mailbox
import org.apache.james.mailbox.MailboxSession
import org.apache.james.mailbox.model.MailboxConstants
import javax.mail.Flags

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 13.01.13 12:35
 */
package object maildir {

  trait NoTransaction extends Mapper {
    def execute[T](tx: Transaction[T]) = tx.run()
  }

  def stripInbox(name: String) = if (name.startsWith(MailboxConstants.INBOX)) {
    name.substring(MailboxConstants.INBOX.length+1)
  } else {
    name
  }

  def stripDelimiter(name: String, delimiter: Char = '.') = if (name.startsWith(delimiter.toString)) {
    name.substring(1)
  } else {
    name
  }

  def flagsToString(flags: Flags) = {
    val buf = new StringBuilder
    if (flags.contains(Flags.Flag.DELETED))
      buf.append("D")
    if (flags.contains(Flags.Flag.ANSWERED))
      buf.append("R")
    if (flags.contains(Flags.Flag.DRAFT))
      buf.append("D")
    if (flags.contains(Flags.Flag.FLAGGED))
      buf.append("F")
    if (flags.contains(Flags.Flag.SEEN))
      buf.append("S")
    if (flags.contains(Flags.Flag.RECENT))
      buf.append("~")
    if (flags.contains(Flags.Flag.USER))
      buf.append("u")
    buf.toString()
  }
}
