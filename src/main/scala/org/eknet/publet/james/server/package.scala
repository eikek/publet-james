package org.eknet.publet.james

import org.apache.commons.configuration.HierarchicalConfiguration
import org.eknet.publet.james.util.MakeCertificate
import org.apache.james.filesystem.api.FileSystem
import org.apache.james.protocols.pop3.POP3Session
import org.apache.james.protocols.api.Response
import org.apache.james.protocols.api.handler.ProtocolHandler
import org.apache.james.protocols.smtp.SMTPSession
import org.apache.james.protocols.smtp.hook.{Hook, HookResult}

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 06.12.12 13:23
 */
package object server {

  case class SmtpBlacklistEvent(ip: String) extends JamesEvent
  case class Pop3BlacklistEvent(ip: String) extends JamesEvent
  case class ImapBlacklistEvent(ip: String) extends JamesEvent

  case class Pop3HandlerEvent(session: POP3Session, response: Response, execTime: Long, handler: ProtocolHandler) extends JamesEvent
  case class SmtpHandlerEvent(session: SMTPSession, response: Response, execTime: Long, handler: ProtocolHandler) extends JamesEvent
  case class SmtpHookEvent(session: SMTPSession, result: HookResult, execTime: Long, hook: Hook) extends JamesEvent

  def createCertificate(configs: Seq[HierarchicalConfiguration], fs: FileSystem) {
    for (config <- configs) {
      val useStartTLS = config.getBoolean("tls.[@startTLS]", false)
      val useSSL = config.getBoolean("tls.[@socketTLS]", false)
      if (useSSL || useStartTLS) {
        val keystore = config.getString("tls.keystore")
        val file = fs.getFile(keystore)
        synchronized {
          if (!file.exists()) {
            file.getParentFile.mkdirs()
            val secret = config.getString("tls.secret", "")
            MakeCertificate.generateSelfSignedCertificate("localhost", file, secret)
          }
        }
      }
    }
  }
}
