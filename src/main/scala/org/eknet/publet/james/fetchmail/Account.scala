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
case class Account(login: String, host: String, user: String, password: String, runInterval: Int) {
  host.ensuring(h => h != null && !h.isEmpty, "No host given")
  user.ensuring(h => h != null && !h.isEmpty, "No user given")
  login.ensuring(h => h != null && !h.isEmpty, "No login given")
  password.ensuring(h => h != null && !h.isEmpty, "No password given")
  runInterval.ensuring(_ > 0, "Invalid runInterval.")
}
