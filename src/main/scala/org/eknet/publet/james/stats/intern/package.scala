package org.eknet.publet.james.stats

import org.apache.james.protocols.pop3.POP3Session
import org.apache.james.protocols.api.Response
import org.apache.james.protocols.api.handler.ProtocolHandler
import org.eknet.publet.james.JamesEvent
import org.apache.james.protocols.smtp.SMTPSession
import org.apache.james.protocols.smtp.hook.{Hook, HookResult}

/**
 * Classes in this package are responsible for translating responses and events from James Hooks and
 * Handlers to [[org.eknet.publet.james.JamesEvent]]s.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 10.01.13 13:55
 */
package object intern {

  case class Pop3HandlerEvent(session: POP3Session, response: Response, execTime: Long, handler: ProtocolHandler) extends JamesEvent

  case class SmtpHandlerEvent(session: SMTPSession, response: Response, execTime: Long, handler: ProtocolHandler) extends JamesEvent
  case class SmtpHookEvent(session: SMTPSession, result: HookResult, execTime: Long, hook: Hook) extends JamesEvent
}
