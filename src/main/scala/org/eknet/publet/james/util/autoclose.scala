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

package org.eknet.publet.james.util

import java.io.IOException

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 04.02.13 20:11
 */
object autoclose {

  class RicherClosable(val ac: AutoCloseable) {

    def exec[A](body: => A): A = {
      try {
        body
      } finally {
        try {
          ac.close()
        } catch {
          case e: IOException =>
        }
      }
    }
  }

  implicit def enrichtClosable(ac: AutoCloseable) = new RicherClosable(ac)
  implicit def unrichClosable(rc: RicherClosable) = rc.ac
}
