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

package org.eknet.publet.james.stats

import com.google.inject.{Inject, Singleton}
import com.google.common.eventbus.Subscribe
import grizzled.slf4j.Logging
import java.util.Date
import org.apache.james.pop3server.core.PassCmdHandler
import org.apache.james.protocols.pop3.POP3Response
import org.eknet.publet.james.server.{Pop3BlacklistEvent, Pop3HandlerEvent}
import com.google.inject.name.Named

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 01.02.13 20:59
 */
@Singleton
class Pop3StatsCollector @Inject() (@Named("connectionCounter") tree: CounterTree) extends LoginStatsService with Logging {

  val stats = new SimpleStats("pop3", tree)

  @Subscribe
  def onBlockedConnection(ev: Pop3BlacklistEvent) {
    stats.count("blockedConnections")
  }

  @Subscribe
  def onPop3Response(ev: Pop3HandlerEvent) {
    ev.handler match {
      case ph: PassCmdHandler => {
        val success = ev.response.getRetCode == POP3Response.OK_RESPONSE
        stats.countLogin(ev.session.getUser, success, Some(ev.session.getRemoteAddress.getAddress.getHostAddress))
      }
      case _ =>
    }
  }

  def getSince = new Date(stats.created.get())
  def getSuccessfulLogins(user: String) = stats.getSuccessfulLogins(user)
  def getFailedLogins(user: String) = stats.getFailedLogins(user)
  def getUsernames = stats.getAllUsers.toArray
  def getIpAddresses = stats.getIpAddresses.toArray
  def getFailedLoginsByIp(ip: String) = stats.getFailedLoginsByIp(ip)
  def getSuccessfulLoginsByIp(ip: String) = stats.getSuccessfulLoginsByIp(ip)

  def getBlockedConnections = stats.getCount("blockedConnections")
  def getSuccessfulLogins = stats.getSuccessfulLogins
  def getFailedLogins = stats.getFailedLoginAttempts
  def reset() {
    stats.reset()
  }
}
