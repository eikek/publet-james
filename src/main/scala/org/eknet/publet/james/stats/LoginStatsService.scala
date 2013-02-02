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

  def getUsernames: Array[String]

  def getSuccessfulLogins: Long
  def getFailedLogins: Long

  def getSince: Date

  def reset()
}
