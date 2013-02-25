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

import grizzled.slf4j.Logging
import com.google.inject.{Inject, Singleton}
import org.eknet.publet.james.data.MailDb

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 20:37
 */
@Singleton
class FetchmailAccounts @Inject() (maildb: MailDb) extends FetchmailAccountsMBean with Logging {

  def getAccounts(login: String) = maildb.getAccountsForLogin(login).map(accountToString).toArray


  def updateAccount(login: String, user: String, host: String, ssl: Boolean, password: String, interval: Int, active: Boolean) {
    val account = Account(login, host, ssl, user, password, interval, active)
    maildb.updateAccount(account)
  }

  def deleteAccount(user: String, host: String) {
    maildb.deleteAccount(user, host)
  }

  def setActive(user: String, host: String, active: Boolean) {
    val acc = maildb.findAccount(user, host)
    acc.map(a => maildb.updateAccount(a.copy(active = active)))
  }

  private[this] def accountToString(account: Account) = {
    "'"+ account.user+"@"+account.host + "' is '" +
      account.login + "' here. "+
      (if (account.ssl) "with SSL" else "") +
      "Updates every "+
      account.runInterval + " runs; active=" +
      account.active+
      "."
  }
}
