package org.eknet.publet.james.stats

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 15:17
 */
class SmtpStats {

  val created = new AtomicLong(System.currentTimeMillis())
  private val counters = new ConcurrentHashMap[SmtpStats.Keys.Key, AtomicLong]()
  val loginStats = new LoginStats

  def count(key: SmtpStats.Keys.Key) {
    val c = counters.putIfAbsent(key, new AtomicLong(1))
    if (c != null) {
      c.incrementAndGet()
    }
  }

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

    val acceptedMails, connections, unknownUser, relayDenied, localDelivery, remoteDelivery = Value
  }
}