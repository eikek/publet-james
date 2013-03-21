/*
 * Copyright 2013 Eike Kettner
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

package org.eknet.publet.james.server

import org.eknet.publet.james.data.MailDb
import com.google.inject.{Inject, Singleton}
import grizzled.slf4j.Logging
import javax.management.MXBean

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.03.13 11:20
 */
trait ConnectionBlacklist {

  def addIp(ip: String)
  def removeIp(ip: String)
  def listIps: List[String]
  def isBlacklisted(ip: String): Boolean
}

@MXBean
trait ConnectionBlacklistMBean {
  def addIp(ip: String)
  def removeIp(ip: String)
  def isBlacklisted(ip: String): Boolean
  def listBlacklisted: Array[String]
}

@Singleton
class ConnectionBlacklistImpl @Inject() (maildb: MailDb) extends ConnectionBlacklist with ConnectionBlacklistMBean {
  def addIp(ip: String) {
    maildb.addToBlacklist(ip)
  }

  def removeIp(ip: String) {
    maildb.removeFromBlacklist(ip)
  }

  def listIps = maildb.getBlacklistedIps

  def isBlacklisted(ip: String) = maildb.isInBlacklist(ip)

  def listBlacklisted = listIps.toArray
}
