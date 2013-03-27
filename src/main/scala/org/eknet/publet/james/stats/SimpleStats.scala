package org.eknet.publet.james.stats

import java.util.concurrent.atomic.AtomicLong
import javax.annotation.concurrent.ThreadSafe
import org.eknet.publet.vfs.Path
import org.eknet.county.County

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 15:18
 */
@ThreadSafe
class SimpleStats(protocol: String, root: County) {

  val county = root("publet-james")(protocol)
  val created = new AtomicLong(System.currentTimeMillis())

  def countLogin(username: String, success: Boolean, ip: Option[String]) {
    county("loginstats.byusername")(username)(success.toString).increment()
    if (ip.isDefined) {
      county("loginstats.byip")(ip.get.replace('.', '-'))(success.toString).increment()
    }
  }

  def count(key: String) {
    count(key, 1L)
  }

  def count(key: String, value: Long) {
    county(key.replace('.', '-')).add(value)
  }

  def getCount(key: String) = county(key).totalCount

  def getFailedLogins(username: String) = county("loginstats.byusername")(username)("false").totalCount
  def getSuccessfulLogins(username: String) = county("loginstats.byusername")(username)("true").totalCount

  def getFailedLoginAttempts = {
    county("loginstats.byusername.*.false").totalCount
  }

  def getSuccessfulLogins = {
    county("loginstats.byusername.*.true").totalCount
  }

  def getAllUsers = {
    county("loginstats.byusername").children.toList.sorted
  }

  def getIpAddresses = {
    county("loginstats.byip").children.map(_.replace('-', '.')).toList.sorted
  }

  def getFailedLoginsByIp(ip: String) = county("loginstats.byip")(ip.replace('.', '-'))("false").totalCount

  def getSuccessfulLoginsByIp(ip: String) = county("loginstats.byip")(ip.replace('.', '-'))("true").totalCount

  def reset() {
    county.reset()
    created.set(System.currentTimeMillis())
  }
}
