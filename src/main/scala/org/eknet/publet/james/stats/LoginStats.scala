package org.eknet.publet.james.stats

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.ThreadSafe

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 15:18
 */
@ThreadSafe
class LoginStats {

  val created = new AtomicLong(System.currentTimeMillis())
  private val succesCounter = new ConcurrentHashMap[String, AtomicLong]()
  private val failedCounter = new ConcurrentHashMap[String, AtomicLong]()

  def countLogin(username: String, success: Boolean) {
    val counter = if (success) {
      succesCounter.putIfAbsent(username, new AtomicLong(1))
    } else {
      failedCounter.putIfAbsent(username, new AtomicLong(1))
    }

    if (counter != null) {
      counter.incrementAndGet()
    }
  }

  def getFailedLogins(username: String) = Option(failedCounter.get(username)).map(_.get())
  def getSuccessfulLogins(username: String) = Option(succesCounter.get(username)).map(_.get())

  def getFailedLoginAttempts = {
    import collection.JavaConversions._
    failedCounter.values().map(_.get()) match {
      case Nil => 0
      case list@_ => list.reduce(_ + _)
    }
  }

  def getSuccessfulLogins = {
    import collection.JavaConversions._
    succesCounter.values().map(_.get()) match {
      case Nil => 0
      case list@_ => list.reduce(_ + _)
    }
  }
  def getAllUsers = {
    import collection.JavaConversions._
    succesCounter.keySet().toSet ++ failedCounter.keys().toSet
  }

  def reset() {
    succesCounter.clear()
    failedCounter.clear()
    created.set(System.currentTimeMillis())
  }
}
