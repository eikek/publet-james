package org.eknet.publet.james.stats

import com.google.inject.Singleton
import com.google.common.eventbus.Subscribe
import org.apache.james.protocols.smtp.hook.{HookReturnCode, HookResult}
import org.apache.james.smtpserver.{AuthRequiredToRelayRcptHook, SendMailHandler, UsersRepositoryAuthHook}
import org.apache.james.protocols.smtp.core.WelcomeMessageHandler
import org.apache.james.smtpserver.fastfail.ValidRcptHandler
import org.eknet.publet.james.mailets.IncomeMailEvent
import java.util.Date
import org.apache.mailet.{MailetContext, MailAddress}
import org.eknet.publet.vfs.util.ByteSize
import org.eknet.publet.james.server.{SmtpHookEvent, SmtpHandlerEvent}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 13:25
 */
@Singleton
class SmtpStatsCollector extends SmtpStatsService {
  import SmtpStats.Keys._

  private val stats = new SmtpStats

  @Subscribe
  def onSmtpHandlerMessage(ev: SmtpHandlerEvent) {
    ev.handler match {
      case welcome: WelcomeMessageHandler => stats.count(connections)
      case _ =>
    }
  }

  @Subscribe
  def onSmtpHookMessage(ev: SmtpHookEvent) {
    ev.hook match {
      case auth: UsersRepositoryAuthHook => {
        val success = ev.result.getResult == HookReturnCode.OK
        val login = Option(ev.session.getUser).getOrElse("dummy")
        stats.loginStats.countLogin(login, success)
      }
      case rcpvalid: ValidRcptHandler => {
        if (ev.result.getResult == HookReturnCode.DENY) {
          stats.count(unknownUser)
        }
      }
      case sendmail: SendMailHandler => {
        if (ev.result.getResult == HookReturnCode.OK) {
          stats.count(acceptedMails)
        }
      }
      case authreq: AuthRequiredToRelayRcptHook => {
        if (ev.result.getResult == HookReturnCode.DENY) {
          stats.count(relayDenied)
        }
      }
      case _ =>
    }
  }

  @Subscribe
  def onMail(ev: IncomeMailEvent) {
    import collection.JavaConversions._
    val size = Option(ev.mail.getMessageSize).getOrElse(0L)
    for (addr <- ev.mail.getRecipients) {
      if (isLocalAddress(addr, ev.config.getMailetContext)) {
        stats.count(localDeliverySize, size)
        stats.count(localDelivery)
      } else {
        stats.count(remoteDeliverySize, size)
        stats.count(remoteDelivery)
      }
    }
  }

  private[this] def isLocalAddress(addr: MailAddress, ctx: MailetContext) = {
    ctx.isLocalServer(addr.getDomain.toLowerCase)
  }

  def reset() {
    stats.clear()
  }

  def getSuccessfulLogins(user: String) = stats.loginStats.getSuccessfulLogins(user).getOrElse(0L)
  def getFailedLogins(user: String) = stats.loginStats.getFailedLogins(user).getOrElse(0L)
  def getUsernames = stats.loginStats.getAllUsers.toArray

  def getSince = new Date(stats.created.get())
  def getSuccessfulLogins = stats.loginStats.getSuccessfulLogins
  def getFailedLogins = stats.loginStats.getFailedLoginAttempts
  def getConnectionAttempts = stats.getCount(connections)
  def getAcceptedMails = stats.getCount(acceptedMails)
  def getUnknownLocalUser = stats.getCount(unknownUser)
  def getRelayDenies = stats.getCount(relayDenied)
  def getLocalDeliveries = stats.getCount(localDelivery)
  def getRemoteDeliveries = stats.getCount(remoteDelivery)
  def getAcceptedMailsBytes = stats.getSizeOfAll
  def getAcceptedMailsSize = ByteSize.bytes.normalizeString(stats.getSizeOfAll)
  def getLocalDeliveredBytes = stats.getLocalDeliverySize
  def getLocalDeliveredSize = ByteSize.bytes.normalizeString(stats.getLocalDeliverySize)
  def getRemoteDeliveredBytes = stats.getRemoteDeliverySize
  def getRemoteDeliveredSize = ByteSize.bytes.normalizeString(stats.getRemoteDeliverySize)
}
