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

package org.eknet.publet.james.data

import org.apache.james.user.api.model.{User => AJUser}
import org.eknet.publet.auth.{Algorithm, PasswordServiceProvider, PubletAuth, User}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 02:03
 */
class JamesUser(u: User, auth: PubletAuth, passwordServiceProv: PasswordServiceProvider) extends AJUser {
  def getUserName = u.login

  def verifyPassword(pass: String) = u.algorithm
    .map(a => passwordServiceProv.forAlgorithm(Algorithm.withName(a.toUpperCase)))
    .map(ps => ps.passwordsMatch(pass, u.password.mkString))
    .getOrElse(pass == u.password.mkString)

  def setPassword(newPass: String) = {
    auth.setPassword(u.login, newPass, None)
    true
  }
}
