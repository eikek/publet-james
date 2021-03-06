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

import lib.PathLock
import org.apache.james.mailbox.{MailboxSession, MailboxPathLocker}
import org.apache.james.mailbox.model.MailboxPath
import org.apache.james.mailbox.MailboxPathLocker.LockAwareExecution
import com.google.inject.{Inject, Singleton}
import java.nio.file.Path

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.01.13 00:39
 */
@Singleton
class MailboxPathLockerImpl @Inject() (store: MaildirStore, lock: PathLock[Path]) extends MailboxPathLocker {

  def executeWithLock[T](session: MailboxSession, path: MailboxPath, callback: LockAwareExecution[T]) =
    executeWithLock(session, path, callback, writeLock = true)

  def executeWithLock[T](session: MailboxSession, mboxPath: MailboxPath, callback: LockAwareExecution[T], writeLock: Boolean) = {
    val path = store.getMaildir(mboxPath).folder
    lock.withLock(path, exclusive = writeLock) {
      callback.execute()
    }
  }
}
