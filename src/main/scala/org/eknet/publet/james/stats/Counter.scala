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

package org.eknet.publet.james.stats

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.03.13 16:57
 */
trait Counter {

  def reset()
  def increment()
  def add(value: Long)
  def totalCount: Long
  def intervalCount: Long
  def resetTime: Long
}

trait CompositeCounter extends Counter {

  def counters: Iterable[Counter]

  def resetTime = counters.foldLeft(Long.MaxValue)((min, c) => if (c.resetTime < min) c.resetTime else min)

  def reset() {
    synchronized {
      counters.foreach(_.reset())
    }
  }

  def increment() {
    synchronized {
      counters.foreach(_.increment())
    }
  }


  def add(value: Long) {
    synchronized {
      counters.foreach(_.add(value))
    }
  }

  def totalCount = synchronized {
    if (counters.isEmpty) 0 else counters.map(_.totalCount).reduce(_ + _)
  }

  def intervalCount = {
    if (counters.isEmpty) 0 else counters.map(_.intervalCount).reduce(_ + _)
  }
}

/**
 * A simple counter. The interval counter is incremented if the last count was not
 * longer that `interval` milliseconds ago.
 *
 * @param interval
 */
class BasicCounter(interval: Long = 5000) extends Counter {
  private var totalCounter = 0L
  private var intervallCounter = 0L
  private var last = System.currentTimeMillis()
  private var resetted = System.currentTimeMillis()

  def reset() {
    synchronized {
      intervallCounter = 0L
      totalCounter = 0L
      last = System.currentTimeMillis()
      resetted = System.currentTimeMillis()
    }
  }

  def increment() { add(1L) }

  def add(value: Long) {
    synchronized {
      totalCounter = totalCounter + value
      val now = System.currentTimeMillis()
      if (now - last <= interval) {
        intervallCounter = intervallCounter + value
      } else {
        intervallCounter = value
      }
      last = System.currentTimeMillis()
    }
  }

  def totalCount = synchronized(totalCounter)
  def resetTime = resetted
  def intervalCount = synchronized(intervallCounter)
}