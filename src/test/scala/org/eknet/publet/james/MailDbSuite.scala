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

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import org.eknet.neoswing.utils.QuickView

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 01.11.12 10:26
 */
class MailDbSuite extends MaildbFixture with ShouldMatchers with BeforeAndAfter {

  test ("add and remove domain node") { maildb =>
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

  test ("double add has no effect") { maildb =>
    maildb.addDomain("mydomain1.com")
    maildb.addDomain("mydomain1.com")
    maildb.getDomainList should have size (1)
    maildb.getDomainList.head should be ("mydomain1.com")
    maildb.containsDomain("mydomain1.com") should be (true)
  }

  test ("add and remove mappings") { maildb =>
    maildb.addMapping("user1", "domain.org", "user2")
    maildb.userDomainMappings("user1", "domain.org") should have size (1)
    maildb.userDomainMappings("user1", "domain.org").head should be ("user2")

    maildb.addMapping("user1", "domain.org", "user3")
    maildb.userDomainMappings("user1", "domain.org") should have size (2)
    maildb.userDomainMappings("user1", "domain.org") should contain ("user2")
    maildb.userDomainMappings("user1", "domain.org") should contain ("user3")
  }

  test ("add and remove blacklist ips") { maildb =>
    maildb.addToBlacklist("127.0.0.1")
    maildb.isInBlacklist("127.0.0.1") should be (true)
    maildb.getBlacklistedIps should have size (1)
    maildb.removeFromBlacklist("127.0.0.1")
    maildb.isInBlacklist("127.0.0.1") should be (false)
    maildb.getBlacklistedIps should have size (0)
  }
}
