package org.eknet.publet.james.stats

import com.google.inject.Singleton
import com.google.common.eventbus.Subscribe
import intern.{SmtpHandlerEvent, SmtpHookEvent}
import org.apache.james.protocols.smtp.hook.{HookReturnCode, HookResult}
import org.apache.james.smtpserver.{AuthRequiredToRelayRcptHook, SendMailHandler, UsersRepositoryAuthHook}
import org.apache.james.protocols.smtp.core.WelcomeMessageHandler
import java.util.concurrent.atomic.AtomicReference
import org.apache.james.smtpserver.fastfail.ValidRcptHandler
import org.eknet.publet.james.mailets.IncomeMailEvent
import java.util.Date
import org.apache.mailet.{MailetContext, MailAddress}

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
    for (addr <- ev.mail.getRecipients) {
      if (isLocalAddress(addr, ev.config.getMailetContext)) {
        stats.count(localDelivery)
      } else {
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
}
