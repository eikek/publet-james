/*
 * Copyright 2012 Eike Kettner
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

package org.apache.james.fetchmail

import javax.mail.MessagingException
import grizzled.slf4j.Logging
import org.apache.james.user.api.UsersRepository
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.queue.api.MailQueue
import org.apache.james.domainlist.api.DomainList

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 18:00
 */
object FetchmailProcessor extends Logging {

  def processAccount(account: ConfiguredAccount) {
    try {
      new StoreProcessor(account).process()
    } catch {
      case e: MessagingException => error("A MessagingException has terminated processing of this Account", e)
    }
  }
}
