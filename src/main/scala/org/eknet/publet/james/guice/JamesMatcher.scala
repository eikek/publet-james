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

package org.eknet.publet.james.guice

import com.google.inject.matcher.{AbstractMatcher, Matchers}
import com.google.inject.TypeLiteral

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.10.12 19:29
 */
object JamesMatcher extends AbstractMatcher[TypeLiteral[_]] {

  private val jamesPackage = Matchers.inSubpackage("org.apache.james")
  private val eknetJamesPackage = Matchers.inSubpackage("org.eknet.publet.james")

  def matches(t: TypeLiteral[_]) =
    jamesPackage.matches(t.getRawType) || eknetJamesPackage.matches(t.getRawType)

}
