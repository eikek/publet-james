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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.util.concurrent.TimeUnit

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 21:49
 */
class MessageNameTest extends FunSuite with ShouldMatchers {

  val validNames = Map(
    "1355675651.f3dd564265174501.foohost,S=661:2," -> MessageName(1355675651, "f3dd564265174501", "foohost", Map("S"->"661"), Nil, ":2,"),
    "1355675588.5c7e107958851103.foohost,S=654:2,S" -> MessageName(1355675588, "5c7e107958851103", "foohost", Map("S" -> "654"), List("S"), ":2,"),
    "1355543030.15049_0.foo.org" -> MessageName(1355543030, "15049_0", "foo.org"),
    "1106685752.12132_0.km1111:2,FRS" -> MessageName(1106685752, "12132_0", "km1111", Map(), List("F", "R", "S"), ":2,"),
    "1334580646.8700_0.km11111:2,S" -> MessageName(1334580646, "8700_0", "km11111", Map(), List("S"), ":2,"),
    "1356958317.V902I69c70fM941470.km20731:2," -> MessageName(1356958317, "V902I69c70fM941470", "km20731", Map(), Nil, ":2,"),
    "1285145964.M884771P11730V0000000000000902I0066C039_2.blups.org,S=2848:2,S" -> MessageName(1285145964, "M884771P11730V0000000000000902I0066C039_2", "blups.org", Map("S"->"2848"), List("S"), ":2,")
  )

  test ("Valid message file names") {
    validNames map { name =>
      MessageName(name._1) should be (name._2)
      MessageName(name._1).fullName should be (name._1)
    }
  }

  test ("parse generated names") {
    for (i <- 1 to 10) {
      val name = MessageName.create()
      MessageName(name.fullName) should be (name)
    }
  }

  test ("parse message names from file") {
    val file = getClass.getResource("/valid-messagenames.txt")
    scala.io.Source.fromURL(file).getLines() foreach {line =>
      val mn = MessageName(line)
      mn.fullName should be (line)
    }
  }

  test ("dont add flags twice") {
    val mn = validNames("1355675588.5c7e107958851103.foohost,S=654:2,S")
    mn.flags should have size (1)
    mn.withFlagSeen.flags should have size (1)
  }

  test ("read attributes in correct order") {
    val mn = MessageName("1355675588.5c7e107958851103.foohost,T=1,H=2,S=654:2,S")
    mn.attributes.size should be (3)
    val keys = for (kv <- mn.attributes) yield kv._1
    keys.toList should be (List("T", "H", "S"))
  }
}
