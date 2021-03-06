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

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 27.01.13 16:38
 */
sealed trait UidRange {

  def start: Long
  def end: Long

}

object UidRange {

  case class Interval(start: Long, end: Long) extends UidRange
  case class Single(id: Long) extends UidRange {
    def start = id
    def end = id
  }
  case class From(start: Long) extends UidRange {
    def end = sys.error("Not available")
  }
  case class Until(end: Long) extends UidRange {
    def start = sys.error("Not available")
  }
  case object All extends UidRange {
    def start = Long.MinValue
    def end = Long.MaxValue
  }

}
