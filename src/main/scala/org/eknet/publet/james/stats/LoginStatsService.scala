package org.eknet.publet.james.stats

import javax.management.MXBean
import java.util.Date

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 17:10
 */
@MXBean
trait LoginStatsService {

  def getSuccessfulLogins(user: String): Long
  def getFailedLogins(user: String): Long
  def getFailedLoginsByIp(ip: String): Long
  def getSuccessfulLoginsByIp(ip: String): Long

  def getUsernames: Array[String]
  def getIpAddresses: Array[String]

  def getSuccessfulLogins: Long
  def getFailedLogins: Long

  def getSince: Date

  def getBlockedConnections: Long
  def reset()
}
