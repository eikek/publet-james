package org.eknet.publet.james.server

import com.google.common.eventbus.EventBus
import org.apache.james.imap.api.process.{ImapSession, ImapProcessor}
import org.apache.james.imap.api.ImapMessage
import org.apache.james.imap.api.process.ImapProcessor.Responder
import org.apache.james.imap.api.message.response.ImapResponseMessage
import org.eknet.publet.james.JamesEvent

/**
 * Wrapping James' [[org.apache.james.imap.api.process.ImapProcessor]] to post
 * events for collecting statistics.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 12:51
 */
class NotifyingImapProcessor(bus: EventBus, self: ImapProcessor) extends ImapProcessor {

  def process(msg: ImapMessage, responder: Responder, session: ImapSession) {
    val wrapped = new NotifyingResponder(msg, responder)
    self.process(msg, wrapped, session)
  }

  class NotifyingResponder(in: ImapMessage, val self: Responder) extends Responder with Proxy {
    def respond(resp: ImapResponseMessage) {
      bus.post(new ImapResponseEvent(in, resp))
      self.respond(resp)
    }
  }
}

case class ImapResponseEvent(request: ImapMessage, response: ImapResponseMessage) extends JamesEvent
