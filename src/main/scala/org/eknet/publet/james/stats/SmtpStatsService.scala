package org.eknet.publet.james.stats

import javax.management.MXBean
import java.util.Date

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 15:46
 */
@MXBean
trait SmtpStatsService extends LoginStatsService {

  def getConnectionAttempts: Long

  def getAcceptedMails: Long
  def getAcceptedMailsBytes: Long
  def getAcceptedMailsSize: String

  def getLocalDeliveries: Long
  def getLocalDeliveredBytes: Long
  def getLocalDeliveredSize: String

  def getRemoteDeliveries: Long
  def getRemoteDeliveredBytes: Long
  def getRemoteDeliveredSize: String

  def getUnknownLocalUser: Long
  def getRelayDenies: Long

  def getSince: Date
}
