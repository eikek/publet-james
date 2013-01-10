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
        stats.countLogin(success)
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
      if (ev.config.getMailetContext.isLocalEmail(addr)) {
        stats.count(localDelivery)
      } else {
        stats.count(remoteDelivery)
      }
    }
  }

  def clearValues() {
    stats.clear()
  }


  def getSince = new Date(stats.created.get())
  def getSuccessfulLogins = stats.getSuccessfulLogins
  def getFailedLogins = stats.getFailedLogins
  def getConnectionAttempts = stats.getCount(connections)
  def getAcceptedMails = stats.getCount(acceptedMails)
  def getUnknownLocalUser = stats.getCount(unknownUser)
  def getRelayDenies = stats.getCount(relayDenied)
  def getLocalDeliveries = stats.getCount(localDelivery)
  def getRemoteDeliveries = stats.getCount(remoteDelivery)
}
