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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 11.01.13 00:31
 */
trait PathLock[Path] {

  type Timeout = (Long, TimeUnit)

  def withLock[A](path: Path, timeout: Timeout = (30, TimeUnit.SECONDS), exclusive: Boolean = true)(fun: => A): A= {
    lock(path, timeout, exclusive)
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

  private val locks = new ConcurrentHashMap[Path, ReentrantReadWriteLock]()

  private[lib] def containsLocks = !locks.isEmpty


  /**
   * Returns whether the current thread holds an exclusive lock for `p`.
   * @param p
   * @return
   */
  def isLockedExclusivelyByCurrentThread(p: Path) = {
    Option(locks.get(p)).map(l => l.isWriteLockedByCurrentThread).getOrElse(false)
  }

  def lock(path: Path, timeout: Timeout, excl: Boolean) {
    val l = new ReentrantReadWriteLock()
    val lock = Option(locks.putIfAbsent(path, l)).getOrElse(l)
    val success = if (excl) lock.writeLock().tryLock(timeout._1, timeout._2)
                  else lock.readLock().tryLock(timeout._1, timeout._2)
    if (!success) {
      throw new TimeoutException(Thread.currentThread()+" timed out waiting to lock '"+ path+"'")
    }
  }

  def unlock(path: Path) {
    Option(locks.get(path)).map(unlock(path, _))
  }

  private[this] def unlock(path: Path, lock: ReentrantReadWriteLock) {
    if (lock.isWriteLockedByCurrentThread) {
      if (lock.getWriteHoldCount == 1 && !lock.hasQueuedThreads) {
        locks.remove(path)
      }
      lock.writeLock().unlock()
    } else {
      if (lock.getReadHoldCount == 1 && !lock.hasQueuedThreads) {
        locks.remove(path)
      }
      lock.readLock().unlock()
    }
  }

}
