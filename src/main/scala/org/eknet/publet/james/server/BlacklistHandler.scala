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

import org.apache.james.protocols.api.handler.{ProtocolHandler, ProtocolHandlerResultHandler}
import org.apache.james.protocols.api.{ProtocolSession, Response}
import com.google.inject.Inject
import org.apache.james.protocols.smtp.{SMTPSession, SMTPRetCode, SMTPResponse}
import org.apache.james.protocols.smtp.dsn.DSNStatus
import org.apache.james.protocols.pop3.{POP3Session, POP3Response}
import grizzled.slf4j.Logging
import com.google.common.eventbus.EventBus

/**
 * Hooks into the pop3 and smtp server and returns error responses for blacklisted
 * ips. Imap is not handled by this subsystem.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.03.13 11:56
 */
class BlacklistHandler @Inject() (blacklist: ConnectionBlacklist, bus: EventBus) extends ProtocolHandlerResultHandler[Response, ProtocolSession] with Logging {

  def onResponse(session: ProtocolSession, response: Response, exectime: Long, handler: ProtocolHandler) = {
    val ip = session.getRemoteAddress.getAddress.getHostAddress
    if (blacklist.isBlacklisted(ip)) {
      session match {
        case smtp: SMTPSession => {
          warn("Deny smtp access for '"+ip+"'. Blacklisted.")
          bus.post(new SmtpBlacklistEvent(ip))
          smtpResponse
        }
        case pop: POP3Session => {
          warn("Deny pop3 access for '"+ip+"'. Blacklisted.")
          bus.post(new Pop3BlacklistEvent(ip))
          pop3Response
        }
        case _ => {
          error("Unrecognized session instance '"+session.getClass+"' cannot apply blacklist filter.")
          response
        }
      }
    } else {
      response
    }
  }

  val smtpResponse = new SMTPResponse(SMTPRetCode.SERVICE_NOT_AVAILABLE,
    DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.UNDEFINED_STATUS) + " Not allowed")

  val pop3Response = POP3Response.ERR
}
