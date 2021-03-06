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

import org.apache.james.mailbox.store.MailboxSessionMapperFactory
import org.apache.james.mailbox.MailboxSession
import com.google.inject.{Inject, Singleton}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.01.13 00:35
 */
@Singleton
class MaildirSessionMapperFactory @Inject() (store: MaildirStore) extends MailboxSessionMapperFactory[Int] {

  def createMessageMapper(session: MailboxSession) = new MaildirMessageMapper(store, session)

  def createMailboxMapper(session: MailboxSession) = new MaildirMailboxMapper(session, store)

  def createSubscriptionMapper(session: MailboxSession) = new MaildirSubscriptionMapper(store)
}
