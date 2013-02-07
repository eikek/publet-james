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

import lib.{MessageFile, MessageName}
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.nio.file.{Files, Paths}
import org.apache.james.mailbox.store.streaming.CountingInputStream

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 07.02.13 14:30
 */
class MaildirMessageTest extends FunSuite with ShouldMatchers {

  import MessageProvider._

  test ("Parse message with LFLF ") {
    val message = MaildirMessage.from(2, readMessage(testmailLfLf))
    message.getBodyStartOctet should be (525)
    val cin = new CountingInputStream(message.getHeaderContent)
    cin.readAll()
    cin.getOctetCount should be (525)
  }

  test ("Parse message with CRLF") {
    val message = MaildirMessage.from(2, readMessage(testmailCrLf))
    message.getBodyStartOctet should be (666)
    val cin = new CountingInputStream(message.getHeaderContent)
    cin.readAll()
    cin.getOctetCount should be (666)
  }

}
