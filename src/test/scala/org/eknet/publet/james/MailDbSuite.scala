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

import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import org.eknet.publet.james.data.MailDb
import org.eknet.publet.ext.orient.GraphDb

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 01.11.12 10:26
 */
class MailDbSuite extends FunSuite with ShouldMatchers with BeforeAndAfter {

  val provider = new TestGraphDbProvider
  var maildb: MailDb = _

  before {
    this.maildb = new MailDb(new GraphDb(provider.getNext))
  }

  test ("add and remove domain node") {
    maildb.addDomain("mydomain.com")
    maildb.getDomainList should have size (1)
    maildb.getDomainList.head should be ("mydomain.com")

    maildb.removeDomain("mydomain.com")
    maildb.getDomainList should have size (0)

    maildb.addDomain("mydomain1.com")
    maildb.addDomain("mydomain2.com")
    maildb.removeDomain("mydomain1.com")
    maildb.getDomainList should have size (1)
    maildb.getDomainList.head should be ("mydomain2.com")
    maildb.containsDomain("mydomain2.com") should be (true)
    maildb.containsDomain("mydomain1.com") should be (false)
  }

  test ("double add has no effect") {
    maildb.addDomain("mydomain1.com")
    maildb.addDomain("mydomain1.com")
    maildb.getDomainList should have size (1)
    maildb.getDomainList.head should be ("mydomain1.com")
    maildb.containsDomain("mydomain1.com") should be (true)
  }

  test ("add and remove mappings") {
    maildb.addMapping("user1", "domain.org", "user2")
    maildb.userDomainMappings("user1", "domain.org") should have size (1)
    maildb.userDomainMappings("user1", "domain.org").head should be ("user2")

    maildb.addMapping("user1", "domain.org", "user3")
    maildb.userDomainMappings("user1", "domain.org") should have size (2)
    maildb.userDomainMappings("user1", "domain.org") should contain ("user2")
    maildb.userDomainMappings("user1", "domain.org") should contain ("user3")
  }
}
