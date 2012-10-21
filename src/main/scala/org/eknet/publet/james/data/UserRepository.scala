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

import org.apache.james.user.api.model.User
import com.google.inject.{Inject, Singleton}
import org.apache.james.user.api.UsersRepository
import org.eknet.publet.auth.{PasswordServiceProvider, PubletAuth}
import collection.JavaConversions._

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 01:59
 */
@Singleton
class UserRepository @Inject() (auth: PubletAuth, psp: PasswordServiceProvider) extends UsersRepository {
  def addUser(username: String, password: String) {}

  def getUserByName(name: String) = auth.findUser(name)
    .map(u => new JamesUser(u, auth, psp))
    .orNull

  def updateUser(user: User) {}

  def removeUser(name: String) {}

  def contains(name: String) = auth.findUser(name).isDefined

  def test(name: String, password: String) = auth.findUser(name)
    .map(u => new JamesUser(u, auth, psp))
    .map(u => u.verifyPassword(password))
    .getOrElse(false)

  def countUsers() = auth.getAllUser.size

  def list() = auth.getAllUser.map(_.login).iterator

  def supportVirtualHosting() = false
}
