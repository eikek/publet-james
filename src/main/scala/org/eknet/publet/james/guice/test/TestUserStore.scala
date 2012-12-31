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

package org.eknet.publet.james.guice.test

import com.google.inject.{Inject, Singleton}
import org.eknet.publet.auth.store.{PermissionStore, UserProperty, User, UserStoreAdapter}
import org.eknet.publet.james.Permissions
import org.eknet.publet.web.{RunMode, Config}

/**
 * A store for test users convenient while developing.
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 01.11.12 14:35
 */
@Singleton
class TestUserStore @Inject() (config: Config) extends UserStoreAdapter with PermissionStore {

  private val users = if (config.mode != RunMode.development) Nil
    else List(createUser("eike"), createUser("john"), createUser("admin"))

  private[this] def createUser(login: String) = {
    new User(login, Map(UserProperty.password -> "test"))
  }

  override def findUser(login: String) = users.find(p => p.login == login)
  override def allUser = users
  override def userOfGroups(groups: String*) = if (groups.toSet.contains(Permissions.mailgroup)) users else Nil
  override def getGroups(login: String) = if (findUser(login).isDefined) allGroups else Set[String]()
  override def allGroups = Set(Permissions.mailgroup)

  def addPermission(group: String, perm: String) {}
  def dropPermission(group: String, perm: String) {}

  def getPermissions(group: String*) = group.flatMap(g => findUser(g)).flatMap(u => getUserPermissions(u.login)).toSet
  def getUserPermissions(login: String) = findUser(login)
    .map(_.login)
    .map(x => x match  {
    case "admin" => Set("*")
    case _ => Set("james:alias:*:"+x, "james:fetchmail:account:*:"+x, "james:sieve:*:"+x)
  }).getOrElse(Set[String]())
}
