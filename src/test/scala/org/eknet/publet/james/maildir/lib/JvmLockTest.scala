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

import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import java.util.concurrent.{Future, CountDownLatch, Executors, TimeUnit}
import collection.mutable.ListBuffer

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 07.02.13 10:56
 */
class JvmLockTest extends FunSuite with ShouldMatchers with BeforeAndAfter {

  private val lock = new JvmLocker[String]()
  private val tenMillis: JvmLocker[String]#Timeout = (10, TimeUnit.MILLISECONDS)
  private val fiveSeconds: JvmLocker[String]#Timeout = (5, TimeUnit.SECONDS)

  test ("exclusive lock is reentrant") {
    lock.isLockedExclusivelyByCurrentThread("path") should be (false)
    lock.withLock("path", tenMillis) {
      lock.isLockedExclusivelyByCurrentThread("path") should be (true)
      lock.withLock("path", tenMillis) {
        lock.isLockedExclusivelyByCurrentThread("path") should be (true)
      }
      lock.isLockedExclusivelyByCurrentThread("path") should be (true)
    }
    lock.isLockedExclusivelyByCurrentThread("path") should be (false)
  }

  test ("shared lock is reentrant") {
    lock.withLock("path", exclusive = false) {
      lock.containsLocks should be (true)
      lock.withLock("path", exclusive = false) {
        lock.containsLocks should be (true)
      }
      lock.containsLocks should be (true)
    }
    lock.containsLocks should be (false)
  }

  test ("exclusive lock") {
    val executor = Executors.newFixedThreadPool(2)
    def createTasks(peng: CountDownLatch, firstExcl: Boolean, secondExcl: Boolean) = {
      val futures = new ListBuffer[Future[_]]()
      val peng2 = new CountDownLatch(1)
      val firstTask = new CountDownLatch(2)
      val secondTask = new CountDownLatch(2)
      futures += executor.submit {
        peng.await(tenMillis._1, tenMillis._2)
        lock.withLock("path", fiveSeconds, exclusive = firstExcl) {
          lock.isLockedExclusivelyByCurrentThread("path") should be (firstExcl)
          peng2.countDown()
          Thread.sleep(60)
          secondTask.getCount should (be (2) or be (0))
          firstTask.countDown()
          Thread.sleep(500)
          secondTask.getCount should (be (2) or be (0))
          firstTask.countDown()
        }
      }
      futures += executor.submit {
        peng.await(tenMillis._1, tenMillis._2)
        peng2.await(tenMillis._1, tenMillis._2)
        lock.withLock("path", fiveSeconds, exclusive = secondExcl) {
          lock.isLockedExclusivelyByCurrentThread("path") should be (secondExcl)
          firstTask.getCount should (be (2) or be (0))
          secondTask.countDown()
          Thread.sleep(100)
          firstTask.getCount should (be (2) or be (0))
        }
        secondTask.countDown()
      }
      futures
    }

    //1. shared must wait for exclusive to finish
    var peng = new CountDownLatch(1)
    var futures = createTasks(peng, true, false)
    peng.countDown()
    futures.map(_.get(5, TimeUnit.SECONDS))

    //2. exclusive must wait for shared to finish
    peng = new CountDownLatch(1)
    futures = createTasks(peng, false, true)
    peng.countDown()
    futures.map(_.get(5, TimeUnit.SECONDS))

    executor.shutdown()
    executor.awaitTermination(2, TimeUnit.SECONDS)
  }

  test ("shared lock") {
    val executor = Executors.newFixedThreadPool(2)
    def createTasks(peng: CountDownLatch) = {
      val futures = new ListBuffer[Future[_]]()
      val firstTask = new CountDownLatch(2)
      val secondTask = new CountDownLatch(2)
      val peng2 = new CountDownLatch(1)
      futures += executor.submit {
        peng.await(tenMillis._1, tenMillis._2)
        lock.withLock("path", fiveSeconds, exclusive = false) {
          lock.isLockedExclusivelyByCurrentThread("path") should be (false)
          firstTask.countDown()
          peng2.countDown()
          Thread.sleep(500)
        }
        firstTask.countDown()
      }
      futures += executor.submit {
        peng.await(tenMillis._1, tenMillis._2)
        peng2.await(tenMillis._1, tenMillis._2)
        lock.withLock("path", fiveSeconds, exclusive = false) {
          lock.isLockedExclusivelyByCurrentThread("path") should be (false)
          secondTask.countDown()
          firstTask.getCount should be (1)
          Thread.sleep(100)
        }
        secondTask.countDown()
      }
      futures
    }

    val peng = new CountDownLatch(1)
    val futures = createTasks(new CountDownLatch(1))
    peng.countDown()
    futures.map(_.get(5, TimeUnit.SECONDS))

    executor.shutdown()
    executor.awaitTermination(2, TimeUnit.SECONDS)
  }

  test ("cleanup lock map") {
    lock.containsLocks should be (false)
    lock.withLock("path") {
      lock.containsLocks should be (true)
    }
    lock.containsLocks should be (false)
  }

  implicit def toRunnable(f: => Any): Runnable = new Runnable {
    def run() { f }
  }
}
