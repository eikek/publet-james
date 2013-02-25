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
import org.eknet.publet.james.fetchmail.{FetchmailScheduler, Account}
import org.eknet.publet.web.shiro.Security
import org.eknet.publet.web.util.PubletWeb
import xml.{Text, NodeSeq}
import org.eknet.publet.james.Permissions

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 22:52
 */
class ManageFetchmailAccounts extends ScalaScript {
  import org.eknet.publet.web.util.RenderUtils.makeJson

  def serve() = {
    paramLc(actionParam) match {
      case Some("get") => safeCall {
        val login = param("login").getOrElse(Security.username)
        withPerm(Permissions.getFetchmailAccount(login)) {
          makeJson(maildb.getAccountsForLogin(login).map(accountToMap).toList)
        }
      }
      case Some("interval-options") => getIntervalOptions
      case Some("update") => addAccount()
      case Some("delete") => deleteAccount()
      case _ => failure("Unknown command")
    }
  }

  private[this] def getIntervalOptions = {
    authenticated {
      val interval = PubletWeb.instance[FetchmailScheduler].get.getInterval
      val options: NodeSeq = for (i <- 1 to 8) yield {
        val selected = if (i==2) Some(Text("selected")) else None
        <option value={i.toString} selected={selected}>{ (interval * i) + " min" }</option>
      }
      makeJson(Map("options" ->(options.toString())))
    }
  }
  private[this] def deleteAccount() = {
    val user = param("user")
    val host = paramLc("host")
    (user, host) match {
      case (Some(u), Some(h)) => safeCall {
        maildb.findAccount(u, h) flatMap { acc =>
          withPerm(Permissions.removeFetchmailAccount(acc.login)) {
            maildb.deleteAccount(u, h)
            success("Account deleted.")
          }
        }
      }
      case _ => failure("Too less parameters")
    }
  }

  private[this] def addAccount() = {
    val user = param("user")
    val host = paramLc("host")
    (user, host) match {
      case (Some(u), Some(h)) => safeCall {
        val password = param("password").orElse(maildb.findAccount(u, h).map(_.password))
        password match {
          case Some(pw) => {
            val login = param("login").getOrElse(Security.username)
            withPerm(Permissions.addFetchmailAccount(login)) {
              val acc = Account(
                login,
                h,
                boolParam("ssl").getOrElse(false),
                u,
                pw,
                param("runInterval").map(_.toInt).getOrElse(2),
                boolParam("active").getOrElse(false)
              )
              maildb.updateAccount(acc)
              success("Account added")
            }
          }
          case _ => failure("Password is missing.")
        }
      }
      case _ => failure("Too less paramters.")
    }
  }
  private[this] def accountToMap(account: Account) = {
    val interval = PubletWeb.instance[FetchmailScheduler].get.getInterval
    Map(
      "login" -> account.login,
      "host" -> account.host,
      "ssl" -> account.ssl,
      "user" -> account.user,
      "password" -> account.password,
      "runInterval" -> account.runInterval,
      "runIntervalMinutes" -> ((account.runInterval * interval)+" min"),
      "active" -> account.active
    )
  }
}
