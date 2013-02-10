package org.eknet.publet.james.stats

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import org.eknet.publet.vfs.util.ByteSize

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 15:17
 */
class SmtpStats {

  val created = new AtomicLong(System.currentTimeMillis())
  private val counters = new ConcurrentHashMap[SmtpStats.Keys.Key, AtomicLong]()
  val loginStats = new LoginStats

  def count(key: SmtpStats.Keys.Key) {
    count(key, 1L)
  }

  def count(key: SmtpStats.Keys.Key, value: Long) {
    val c = counters.putIfAbsent(key, new AtomicLong(value))
    if (c != null) {
      c.addAndGet(value)
    }
  }

  def getLocalDeliverySize = getCount(SmtpStats.Keys.localDeliverySize)
  def getRemoteDeliverySize = getCount(SmtpStats.Keys.remoteDeliverySize)
  def getSizeOfAll = getLocalDeliverySize + getRemoteDeliverySize

  def getCount(key: SmtpStats.Keys.Key) = Option(counters.get(key)).map(_.get()).getOrElse(0L)

  def clear() {
    counters.clear()
    loginStats.reset()
    created.set(System.currentTimeMillis())
  }
}

object SmtpStats {

  object Keys extends Enumeration {
    type Key = Value

    val acceptedMails,
        connections,
        unknownUser,
        relayDenied,
        localDelivery,
        localDeliverySize,
        remoteDeliverySize,
        remoteDelivery = Value
  }
}