package org.eknet.publet.james.stats

import com.google.inject.Singleton
import org.eknet.publet.james.server.ImapResponseEvent
import com.google.common.eventbus.Subscribe
import org.apache.james.imap.api.message.response.{ImapResponseMessage, StatusResponse}
import org.apache.james.imap.message.request.{IRAuthenticateRequest, LoginRequest}
import org.apache.commons.codec.binary.Base64
import java.util.Date

/**
 * Counts failed and successful logins per user.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 12:55
 */
@Singleton
class ImapStatsCollector extends LoginStatsService {

  val stats = new LoginStats

  @Subscribe
  def onImapResponse(ev: ImapResponseEvent) {
    ev.request match {
      case loginReq: LoginRequest => {
        isSuccessful(ev.response).map(succ => stats.countLogin(loginReq.getUserid, succ))
      }
      case irLoginReq: IRAuthenticateRequest => {
        val decoded = new String(Base64.decodeBase64(irLoginReq.getInitialClientResponse))
        val login = if (decoded.indexOf('\u0000') > 0) {
          Some(decoded.takeWhile(_ != '\u0000'))
        }
        else if (decoded.indexOf('=') > 0) {
          val un = decoded.split("=", 2)(0)
          Some(un)
        }
        else None

        login.map(l => {
          //Strange: successful IR logins are not delegated to the LOGIN command. failed ones are...
          if (isSuccessful(ev.response).exists(_ == true)) {
            stats.countLogin(l, success = true)
          }
        })
      }
      case _ => None
    }
  }

  def isSuccessful(resp: ImapResponseMessage) = resp match {
    case status: StatusResponse => {
      Some(status.getServerResponseType == StatusResponse.Type.OK)
    }
    case _ => None
  }


  def getSince = new Date(stats.created.get())
  def getSuccessfulLogins(user: String) = stats.getSuccessfulLogins(user).getOrElse(0L)
  def getFailedLogins(user: String) = stats.getFailedLogins(user).getOrElse(0L)
  def getUsernames = stats.getAllUsers.toArray
  def getSuccessfulLogins = stats.getSuccessfulLogins
  def getFailedLogins = stats.getFailedLoginAttempts
}
