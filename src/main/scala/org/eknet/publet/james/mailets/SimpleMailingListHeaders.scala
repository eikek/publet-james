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

package org.eknet.publet.james.mailets

import com.google.common.eventbus.Subscribe
import com.google.inject.{Inject, Singleton}
import collection.JavaConversions._
import org.eknet.publet.web.Settings
import org.apache.mailet.base.RFC2822Headers
import java.util.UUID

/**
 * This mailet looks for messages that have exactly one
 * TO address. If that address is configured to be a mailing
 * list, it adds some message headers, to identify the mail
 * as a mail to a mailing list.
 *
 * An address is identified as a mailing list address, if it
 * is mentioned in the settings file using the key
 * `james.mailing-lists`. The value is expected to be
 * a list of email address separated by comma or semi-colon.
 *
 * The recipients of the lists must be configured using
 * virtual address mappings. This mailet just adds some headers
 * to mails sent to lists such that mail clients can make
 * use of it. This allows for a simple but working mailing list
 * setup.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 16.12.12 17:44
 */
@Singleton
class SimpleMailingListHeaders @Inject() (settings: Settings) {

  @Subscribe
  def addListHeaders(event: IncomeMailEvent) {
    val recipients = event.mail.getRecipients
    if (recipients.size() == 1) {
      val to = recipients.head.toString
      if (isMailingList(to)) {
        val msg = event.mail.getMessage
        msg.setHeader(RFC2822Headers.REPLY_TO, to)
        msg.setHeader(RFC2822Headers.SENDER, to)
        if (to.endsWith("@localhost")) {
          val id = to.takeWhile(_ != '@') + "."+ randomId +".localhost"
          msg.setHeader("List-Id", id)
        } else {
          msg.setHeader("List-Id", to)
        }
        msg.setHeader("Precedence", "list")
        msg.saveChanges()
      }
    }
  }

  def isMailingList(mail: String): Boolean = {
    settings("james.mailing-lists").exists(lists => {
      lists.split("[,;]").contains(mail)
    })
  }

  protected def randomId = UUID.randomUUID().toString.replace("-", "")
}
