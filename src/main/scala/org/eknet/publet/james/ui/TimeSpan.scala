/*
 * Copyright 2012 Eike Kettner
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

package org.eknet.publet.james.ui

import java.util.concurrent.TimeUnit

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 22.12.12 14:05
 */
case class TimeSpan(span: Long, unit: TimeUnit) {

  def convert(unit: TimeUnit) = TimeSpan(unit.convert(span, this.unit), unit)

  override lazy val toString = span +" "+ TimeSpan.symbol(unit)

  lazy val humanString = {
    def checkMatch(units: List[TimeUnit]): TimeUnit = units match {
      case u::us => if (convert(u).span>0) u else checkMatch(us)
      case _ => sys.error("Unreachable code")
    }
    val unit = checkMatch(TimeUnit.values().reverse.toList)
    convert(unit).toString
  }
}

object TimeSpan {

  def diff(ts1: Long, ts2: Long) = scala.math.abs(ts1 - ts2)

  def millis(span: Long) = TimeSpan(span, TimeUnit.MILLISECONDS)
  def seconds(span: Long) = TimeSpan(span, TimeUnit.SECONDS)
  def minutes(span: Long) = TimeSpan(span, TimeUnit.MINUTES)
  def days(span: Long) = TimeSpan(span, TimeUnit.DAYS)

  def fromNow(until: Long) = millis(diff(until, System.currentTimeMillis()))

  def symbol(unit: TimeUnit) = unit match {
    case TimeUnit.NANOSECONDS => "ns"
    case TimeUnit.MICROSECONDS => "μs"
    case TimeUnit.MILLISECONDS => "ms"
    case TimeUnit.SECONDS => "s"
    case TimeUnit.MINUTES => "min"
    case TimeUnit.HOURS => "h"
    case TimeUnit.DAYS => "d"
  }

  implicit def timespanToTuple(s: TimeSpan) = (s.span, s.unit)
}
