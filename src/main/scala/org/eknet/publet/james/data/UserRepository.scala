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

import org.apache.james.user.api.model.{User => JAUser}
import com.google.inject.{Inject, Singleton}
import org.apache.james.user.api.{UsersRepositoryException, UsersRepository}
import collection.JavaConversions._
import org.eknet.publet.james.Permissions
import org.eknet.publet.auth.store.{User, DefaultAuthStore}
import org.eknet.publet.auth.PasswordServiceProvider
import org.apache.james.user.api.model.User

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 01:59
 */
@Singleton
class UserRepository @Inject() (auth: DefaultAuthStore, psp: PasswordServiceProvider) extends UsersRepository {

  private def findUser(name: String) = auth.findUser(name).filter(u => auth.getGroups(u.login).contains(Permissions.mailgroup))
  private def allUsers = auth.userOfGroups(Permissions.mailgroup)

  def getUserByName(name: String) = findUser(name)
    .map(u => new JamesUser(u, auth, psp))
    .orNull

  def contains(name: String) = findUser(name).isDefined

  def test(name: String, password: String) = findUser(name)
    .map(u => new JamesUser(u, auth, psp))
    .map(u => u.verifyPassword(password))
    .getOrElse(false)

  def countUsers() = list().size

  def list() = allUsers.map(_.login).iterator

  def supportVirtualHosting() = false

  def addUser(username: String, password: String) {
    throw new UsersRepositoryException("Repository not writeable")
  }

  def updateUser(user: JAUser) {
    throw new UsersRepositoryException("Repository not writeable")
  }

  def removeUser(name: String) {
    throw new UsersRepositoryException("Repository not writeable")
  }

}
