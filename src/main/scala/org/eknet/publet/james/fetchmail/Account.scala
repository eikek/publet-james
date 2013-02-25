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

package org.eknet.publet.james.fetchmail

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 18:54
 */
case class Account(login: String, host: String, ssl: Boolean, user: String, password: String, runInterval: Int, active: Boolean) {
  precondition(host != null && !host.isEmpty, "No host given")
  precondition(user != null && !user.isEmpty, "No user given")
  precondition(login != null && !user.isEmpty, "No login given")
  precondition(password != null && !password.isEmpty, "No password given")
  precondition(runInterval > 0, "Invalid runInterval.")

  private[this] def precondition(expr: Boolean, msg: => Any) {
    if (!expr)
      throw new IllegalArgumentException(msg.toString)
  }
}
