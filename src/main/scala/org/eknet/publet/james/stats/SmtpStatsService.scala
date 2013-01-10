package org.eknet.publet.james.stats

import javax.management.MXBean
import java.util.Date

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 15:46
 */
@MXBean
trait SmtpStatsService {

  def getSuccessfulLogins: Long
  def getFailedLogins: Long

  def getConnectionAttempts: Long

  def getAcceptedMails: Long

  def getUnknownLocalUser: Long
  def getRelayDenies: Long

  def clearValues()

  def getSince: Date
}
