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
import java.nio.file.{Files, Paths}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 07.02.13 19:34
 */
object MessageProvider {

  val testmailLfLf = MessageName("1355543030.15049_0.km20731:2,S")
  val testmailCrLf = MessageName("1360244851.62ad1b340f5e45d8b94a2c907a0b83aa-20373.ithaka,S=1372:2,S")

  val emptyMail = MessageName("1201013920.M667822P28232V000000000000FE0BI0036A4AD_0.binvoll,S=357:2,S")

  def readMessage(name: MessageName) = {
    val url = getClass.getResource("/"+name.fullName)
    require(url != null)
    val path = if (url.toURI.getScheme != "file") {
      Paths.get("target", "scala-2.9.2", "test-classes", name.fullName)
    } else {
      Paths.get(url.toURI)
    }
    val mf = MessageFile(1231L, name, path)
    Files.exists(mf.file).ensuring(_ == true)
    mf
  }
}
