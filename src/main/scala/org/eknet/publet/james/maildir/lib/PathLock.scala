/*
 * Copyright 2013 Eike Kettner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eknet.publet.james.maildir.lib

import java.util.concurrent.{TimeoutException, TimeUnit, ConcurrentHashMap}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.01.13 00:31
 */
trait PathLock[Path] {

  type Timeout = (Long, TimeUnit)

  def withLock[A](path: Path, timeout: Timeout = (30, TimeUnit.SECONDS), exclusive: Boolean = true)(fun: => A): A= {
    lock(path, timeout)
    try {
      fun
    } finally {
      unlock(path)
    }
  }

  def lock(path: Path, timeout: Timeout = (30, TimeUnit.SECONDS), exclusive: Boolean = true)

  def unlock(path: Path)

}

class JvmLocker[Path] extends PathLock[Path] {
  private implicit def timeoutToMillis(timeout: Timeout) = timeout._2.toMillis(timeout._1)

  private val locks = new ConcurrentHashMap[Path, Thread]()
  private val exclusive = new ConcurrentHashMap[Thread, Boolean]()


  def lock(path: Path, timeout: Timeout, excl: Boolean) {
    val current = Thread.currentThread()
    exclusive.putIfAbsent(current, excl)
    Option(locks.putIfAbsent(path, current)) match {
      case Some(t) if (t != current) => {
        if (excl || Option(exclusive.get(t)).exists(_ == true)) {
          val start = System.currentTimeMillis()
          synchronized { wait(timeout) }
          if (System.currentTimeMillis() - start >= timeoutToMillis(timeout)) {
            throw new TimeoutException("lock timed out")
          }
          lock(path, timeout, excl)
        }
      }
      case _ =>
    }
    exclusive.remove(Thread.currentThread())
  }

  def unlock(path: Path) {
    if (Option(locks.remove(path)) != None) {
      synchronized { notifyAll() }
    }
  }

}
