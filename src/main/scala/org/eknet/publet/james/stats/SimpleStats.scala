package org.eknet.publet.james.stats

import java.util.concurrent.atomic.AtomicLong
import javax.annotation.concurrent.ThreadSafe
import org.eknet.publet.vfs.Path

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 15:18
 */
@ThreadSafe
class SimpleStats(protocol: String, tree: CounterTree) {

  protected val rootPath = Path(protocol)
  private val byLoginPath = rootPath / "loginstats" / "bylogin"
  private val byIpsPath = rootPath / "loginstats" / "byipaddress"

  val created = new AtomicLong(System.currentTimeMillis())

  private[this] def pathFor(username: String, success: Boolean) = byLoginPath / username / success.toString

  def countLogin(username: String, success: Boolean, ip: Option[String]) {
    tree.getCompositeCounter(pathFor(username, success)).increment()
    if (ip.isDefined) {
      tree.getCounter(byIpsPath / ip.get / success.toString).increment()
    }
  }

  def count(key: String) {
    count(key, 1L)
  }

  def count(key: String, value: Long) {
    tree.getCounter(rootPath / key).add(value)
  }

  def getCount(key: String) = tree.findCounter(rootPath / key).map(_.totalCount).getOrElse(0L)

  def getFailedLogins(username: String) = tree.getCounter(pathFor(username, success = false)).totalCount
  def getSuccessfulLogins(username: String) = tree.getCounter(pathFor(username, success = true)).totalCount

  def getFailedLoginAttempts = {
    tree.searchCoutners((byLoginPath / "*" / "false").asString).totalCount
  }

  def getSuccessfulLogins = {
    tree.searchCoutners((byLoginPath / "*" / "true").asString).totalCount
  }

  def getAllUsers = {
    tree.getChildren(byLoginPath).toSet
  }

  def getIpAddresses = {
    tree.getChildren(byIpsPath).toList.sorted
  }

  def getFailedLoginsByIp(ip: String) = tree.findCounter(byIpsPath / ip / "false").map(_.totalCount).getOrElse(0L)
  def getSuccessfulLoginsByIp(ip: String) = tree.findCounter(byIpsPath / ip / "true").map(_.totalCount).getOrElse(0L)

  def reset() {
    tree.removeCounter(rootPath)
    created.set(System.currentTimeMillis())
  }
}
