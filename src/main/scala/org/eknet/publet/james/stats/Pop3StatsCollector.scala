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

import com.google.inject.Singleton
import com.google.common.eventbus.Subscribe
import grizzled.slf4j.Logging
import intern.Pop3HandlerEvent
import java.util.Date
import org.apache.james.pop3server.core.PassCmdHandler
import org.apache.james.protocols.pop3.POP3Response

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 01.02.13 20:59
 */
@Singleton
class Pop3StatsCollector extends LoginStatsService with Logging {

  val stats = new LoginStats

  @Subscribe
  def onPop3Response(ev: Pop3HandlerEvent) {
    info(">>>>>> event: "+ ev)
    ev.handler match {
      case ph: PassCmdHandler => {
        val success = ev.response.getRetCode == POP3Response.OK_RESPONSE
        stats.countLogin(ev.session.getUser, success)
      }
      case _ =>
    }
  }

  def getSince = new Date(stats.created.get())
  def getSuccessfulLogins(user: String) = stats.getSuccessfulLogins(user).getOrElse(0L)
  def getFailedLogins(user: String) = stats.getFailedLogins(user).getOrElse(0L)
  def getUsernames = stats.getAllUsers.toArray
  def getSuccessfulLogins = stats.getSuccessfulLogins
  def getFailedLogins = stats.getFailedLoginAttempts
}
