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

package org.eknet.publet.james

import org.apache.james.rrt.lib.{RecipientRewriteTableUtil, AbstractRecipientRewriteTable, AbstractRecipientRewriteTableTest}
import org.slf4j.LoggerFactory
import java.util
import org.apache.james.rrt.api.RecipientRewriteTable
import org.scalatest.junit.AssertionsForJUnit

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 31.10.12 19:00
 */
class RecipientRewriteTableTest extends AbstractRecipientRewriteTableTest with AssertionsForJUnit {

  val logger = LoggerFactory.getLogger(classOf[RecipientRewriteTableTest])

  val REGEX_TYPE: Int = 0
  val ERROR_TYPE: Int = 1
  val ADDRESS_TYPE: Int = 2
  val ALIASDOMAIN_TYPE: Int = 3

  def getRecipientRewriteTable = null
  def addMapping(p1: String, p2: String, p3: String, p4: Int) = false
  def removeMapping(p1: String, p2: String, p3: String, p4: Int) = false
}


