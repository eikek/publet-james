package org.eknet.publet.james.server

import org.apache.james.protocols.api.handler.{ProtocolHandler, ProtocolHandlerResultHandler}
import org.apache.james.protocols.api.{ProtocolSession, Response}
import org.apache.james.protocols.smtp.SMTPSession
import grizzled.slf4j.Logging
import com.google.inject.{Inject, Singleton}
import com.google.common.eventbus.EventBus

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 13:59
 */
@Singleton
class SmtpProtocolEventForward @Inject() (bus: EventBus) extends ProtocolHandlerResultHandler[Response, SMTPSession] with Logging {

  def onResponse(session: ProtocolSession, response: Response, execTime: Long, handler: ProtocolHandler) = {
    session match {
      case smtp: SMTPSession => bus.post(new SmtpHandlerEvent(smtp, response, execTime, handler))
      case _ => warn("SmtpStatsHandler received non-smtp session: "+ session)
    }

    response
  }
}
