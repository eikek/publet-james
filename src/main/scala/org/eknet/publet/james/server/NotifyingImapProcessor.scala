package org.eknet.publet.james.server

import com.google.common.eventbus.EventBus
import org.apache.james.imap.api.process.{ImapSession, ImapProcessor}
import org.apache.james.imap.api.ImapMessage
import org.apache.james.imap.api.process.ImapProcessor.Responder
import org.apache.james.imap.api.message.response.{StatusResponse, ImapResponseMessage}
import org.eknet.publet.james.JamesEvent
import org.apache.james.imapserver.netty.NettyImapSession
import java.net.InetSocketAddress
import org.apache.james.imap.message.response.UnpooledStatusResponseFactory
import org.apache.james.imap.api.display.HumanReadableText

/**
 * Wrapping James' [[org.apache.james.imap.api.process.ImapProcessor]] to post
 * events for collecting statistics.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 12:51
 */
class NotifyingImapProcessor(bus: EventBus, blacklist: ConnectionBlacklist, self: ImapProcessor) extends ImapProcessor {

  private val statusFactory = new UnpooledStatusResponseFactory

  private val failedResponse = statusFactory.bye(HumanReadableText.FAILED)

  def process(msg: ImapMessage, responder: Responder, session: ImapSession) {
    val ip = findIp(session)
    val blacklisted = ip map { ip => blacklist.isBlacklisted(ip) } getOrElse (false)
    val wrapped = if (blacklisted) {
      new FailedResponder(msg, responder, ip.getOrElse("unknown"))
    } else {
      new NotifyingResponder(msg, responder, ip)
    }
    self.process(msg, wrapped, session)
  }

  private[this] def findIp(session: ImapSession): Option[String] = {
    session match {
      case netty: NettyImapSession => netty.getChannel.getRemoteAddress match {
        case inet: InetSocketAddress => Some(inet.getAddress.getHostAddress)
        case _ => None
      }
      case _ => None
    }
  }

  class NotifyingResponder(in: ImapMessage, val self: Responder, ip: Option[String]) extends Responder with Proxy {
    def respond(resp: ImapResponseMessage) {
      bus.post(new ImapResponseEvent(in, resp, ip))
      self.respond(resp)
    }
  }

  class FailedResponder(in: ImapMessage, val self: Responder, ip: String) extends Responder with Proxy {
    def respond(ignored: ImapResponseMessage) {
      if (ignored.isInstanceOf[StatusResponse]) {
        bus.post(new ImapBlacklistEvent(ip))
        self.respond(ignored)
      } else {
        self.respond(failedResponse)
      }
    }
  }
}

case class ImapResponseEvent(request: ImapMessage, response: ImapResponseMessage, ip: Option[String]) extends JamesEvent
