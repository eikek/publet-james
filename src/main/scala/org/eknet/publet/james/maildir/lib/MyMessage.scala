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

package org.eknet.publet.james.maildir.lib

import java.io.OutputStream
import javax.mail.{Message, Flags}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.01.13 20:52
 */
trait MyMessage {

  /**
   * Returns the value for the messages RECENT flag. This is used to
   * determine whether to put the message in NEW or CUR when adding
   * it.
   *
   * @return
   */
  def isRecent: Boolean

  def getSize: Option[Int]

  def getFlags: Flags

  def writeTo(out: OutputStream)
}

class MyMailMessage(val message: Message) extends MyMessage {

  def isRecent = message.isSet(Flags.Flag.RECENT)

  def getSize = message.getSize match {
    case x if (x > -1) => Some(x)
    case _ => None
  }

  def getFlags = message.getFlags

  def writeTo(out: OutputStream) {
    message.writeTo(out)
  }
}