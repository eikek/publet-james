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

  private val testmailLfLf = MessageName("1355543030.15049_0.km20731:2,S")
  private val testmailCrLf = MessageName("1360244851.62ad1b340f5e45d8b94a2c907a0b83aa-20373.ithaka,S=1372:2,S")

  private def readMessage(name: MessageName) = {
    val url = getClass.getResource("/"+name.fullName)
    url should not be (null)
    val path = if (url.toURI.getScheme != "file") {
      Paths.get("target", "scala-2.9.2", "test-classes", name.fullName)
    } else {
      Paths.get(url.toURI)
    }
    val mf = MessageFile(1231L, name, path)
    Files.exists(mf.file) should be (true)
    mf
  }

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
