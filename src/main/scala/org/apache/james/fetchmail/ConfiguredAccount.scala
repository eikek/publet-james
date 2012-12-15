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

package org.apache.james.fetchmail

import javax.mail.Session
import java.util
import org.apache.mailet.MailAddress
import org.apache.james.user.api.UsersRepository
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.queue.api.MailQueue
import org.apache.james.domainlist.api.DomainList
import org.slf4j.Logger

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 17:57
 */
class ConfiguredAccount(sequenceNumber: Int,
                        parsedConfiguration: ParsedConfiguration,
                        user: String,
                        password: String,
                        recipient: String,
                        ignoreRecipientHeader: Boolean,
                        customRecipientHeader: String,
                        session: Session,
                        var runIntervall: Int)
  extends Account(sequenceNumber, parsedConfiguration, user, password, recipient, ignoreRecipientHeader, customRecipientHeader, session) {

  def this() = this(0, null, null, null, null, false, null, null, 2)

  if ( runIntervall <= 0 ) {
    runIntervall = 2
  }

  def configure(logger: Logger, repo: UsersRepository, dns: DNSService, queue: MailQueue, dnl: DomainList) {
    val config = getParsedConfiguration
    config.setLogger(logger)
    config.setDomainList(dnl)
    config.setMailQueue(queue)
    config.setLocalUsers(repo)
    config.setDNSServer(dns)
    config.setDefaultDomainName(dnl.getDefaultDomain)
    config.setBlacklist("")
  }


  override def getParsedConfiguration =
    super.getParsedConfiguration.asInstanceOf[AccountConfiguration]

  override def setPassword(password: String) {
    super.setPassword(password)
  }

  override def setRecipient(recipient: MailAddress) {
    super.setRecipient(recipient)
  }

  override def setRecipient(recipient: String) {
    super.setRecipient(recipient)
  }

  override def setUser(user: String) {
    super.setUser(user)
  }

  override def setIgnoreRecipientHeader(ignoreRecipientHeader: Boolean) {
    super.setIgnoreRecipientHeader(ignoreRecipientHeader)
  }

  override def setSequenceNumber(sequenceNumber: Int) {
    super.setSequenceNumber(sequenceNumber)
  }

  override def computeDeferredRecipientNotFoundMessageIDs() = super.computeDeferredRecipientNotFoundMessageIDs()

  override def updateDeferredRecipientNotFoundMessageIDs() {
    super.updateDeferredRecipientNotFoundMessageIDs()
  }

  override def setDeferredRecipientNotFoundMessageIDs(defferedRecipientNotFoundMessageIDs: util.List[String]) {
    super.setDeferredRecipientNotFoundMessageIDs(defferedRecipientNotFoundMessageIDs)
  }

  def setParsedConfiguration(parsedConfiguration: AccountConfiguration) {
    super.setParsedConfiguration(parsedConfiguration)
  }

  override def setSession(session: Session) {
    super.setSession(session)
  }
}
