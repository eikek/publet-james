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

import org.apache.james.domainlist.lib.AbstractDomainListTest
import org.slf4j.LoggerFactory
import org.eknet.publet.james.data.{MailDb, PubletDomainList}
import org.eknet.publet.ext.graphdb.GraphDb
import org.eknet.scue.{OrientDbFactory, NamedGraph}
import com.tinkerpop.blueprints.impls.orient.OrientGraph

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 01.11.12 11:07
 */
class PubletDomainListTest extends AbstractDomainListTest {
  val logger = LoggerFactory.getLogger(classOf[PubletDomainListTest])

  val factory = new OrientDbFactory()
  var db: NamedGraph[OrientGraph] = _
  var maildb: MailDb = _

  override def setUp() {
    db = factory.newRandomDb
    maildb = new MailDb(new GraphDb(new OrientGraphWrapper(db)), new NullUsersRepository)
    super.setUp()
  }

  override def tearDown() {
    super.tearDown()
    factory.destroy(db)
    db = null
    maildb = null
  }

  def createDomainList() = {
    val dl = new PubletDomainList(maildb)
    dl.setLog(logger)
    dl.setDNSService(getDNSServer("localhost"))
    dl.setAutoDetect(false)
    dl.setAutoDetectIP(false)
    dl
  }
}
