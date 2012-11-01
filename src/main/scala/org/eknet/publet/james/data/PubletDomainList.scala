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

package org.eknet.publet.james.data

import org.apache.james.domainlist.lib.AbstractDomainList
import com.google.inject.{Inject, Singleton}
import org.apache.james.domainlist.api.DomainListException

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 21.10.12 12:47
 */
@Singleton
class PubletDomainList @Inject() (maildb: MailDb) extends AbstractDomainList {
  import collection.JavaConversions._

  def containsDomain(domain: String) = maildb.containsDomain(domain)
  def addDomain(domain: String) {
    if (containsDomain(domain.toLowerCase))
      throw new DomainListException("Domain '"+domain.toLowerCase+"' alread added.")
    maildb.addDomain(domain)
  }
  def removeDomain(domain: String) { maildb.removeDomain(domain) }
  def getDomainListInternal = maildb.getDomainList
}
