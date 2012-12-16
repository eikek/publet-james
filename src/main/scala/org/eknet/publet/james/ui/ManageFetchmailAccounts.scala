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

package org.eknet.publet.james.ui

import org.eknet.publet.engine.scala.ScalaScript
import org.eknet.publet.james.fetchmail.Account
import org.eknet.publet.web.shiro.Security

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 22:52
 */
class ManageFetchmailAccounts extends ScalaScript {
  import org.eknet.publet.web.util.RenderUtils.makeJson

  def serve() = {
    paramLc(actionParam) match {
      case Some("get") => {
        val login = param("login").getOrElse(Security.username)
        makeJson(maildb.getAccountsForLogin(login).map(accountToMap).toList)
      }
      case Some("update") => addAccount()
      case Some("delete") => deleteAccount()
      case _ => failure("Unknown command")
    }
  }

  private[this] def deleteAccount() = {
    val user = param("user")
    val host = paramLc("host")
    (user, host) match {
      case (Some(u), Some(h)) => {
        maildb.deleteAccount(u, h)
        success("Account deleted.")
      }
      case _ => failure("Too less parameters")
    }
  }

  private[this] def addAccount() = {
    val user = param("user")
    val host = paramLc("host")
    (user, host) match {
      case (Some(u), Some(h)) => {
        val password = param("password").orElse(maildb.findAccount(u, h).map(_.password))
        password match {
          case Some(pw) => {
            val acc = Account(
              param("login").getOrElse(Security.username),
              h, u, pw,
              param("runInterval").map(_.toInt).getOrElse(2),
              boolParam("active").getOrElse(false)
            )
            maildb.updateAccount(acc)
            success("Account added")
          }
          case _ => failure("Password is missing.")
        }
      }
      case _ => failure("Too less paramters.")
    }
  }
  private[this] def accountToMap(account: Account) = Map(
    "login" -> account.login,
    "host" -> account.host,
    "user" -> account.user,
    "password" -> account.password,
    "runInterval" -> account.runInterval,
    "active" -> account.active
  )
}
