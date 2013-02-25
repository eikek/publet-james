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
 * @since 15.12.12 20:37
 */
trait FetchmailAccountsMBean {

  def getAccounts(login: String): Array[String]

  def updateAccount(login: String, user: String, host: String, ssl: Boolean, password: String, interval: Int, active: Boolean)

  def setActive(user: String, host: String, active: Boolean)

  def deleteAccount(user: String, host: String)
}
