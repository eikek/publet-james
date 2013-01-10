package org.eknet.publet.james.stats.intern

import org.apache.james.protocols.api.handler.{ProtocolHandler, ProtocolHandlerResultHandler}
import org.apache.james.protocols.api.{ProtocolSession, Response}
import org.apache.james.protocols.pop3.POP3Session
import grizzled.slf4j.Logging
import com.google.inject.{Inject, Singleton}
import com.google.common.eventbus.EventBus

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 13:55
 */
@Singleton
class Pop3StatsHandler @Inject() (bus: EventBus) extends ProtocolHandlerResultHandler[Response, POP3Session] with Logging {

  def onResponse(session: ProtocolSession, response: Response, execTime: Long, handler: ProtocolHandler) = {
    session match {
      case pop3: POP3Session => bus.post(new Pop3HandlerEvent(pop3, response, execTime, handler))
      case _ => warn("Pop3StatsHandler received non-pop3 session: "+ session)
    }
    response
  }
}
