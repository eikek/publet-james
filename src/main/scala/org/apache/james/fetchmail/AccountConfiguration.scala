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

import org.apache.james.user.api.UsersRepository
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.domainlist.api.DomainList
import org.apache.james.queue.api.MailQueue
import org.slf4j.Logger
import org.apache.commons.configuration.HierarchicalConfiguration
import java.util
import org.apache.mailet.MailAddress

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 18:13
 */
class AccountConfiguration(logger: Logger,
                           localUsers: UsersRepository,
                           dnsServer: DNSService,
                           domainList: DomainList,
                           queue: MailQueue)
  extends ParsedConfiguration(null, logger, localUsers, dnsServer, domainList, queue) {

  def this() = this(null, null, null, null, null)

  override def configure(conf: HierarchicalConfiguration) {
    //no xml config. some defaults
  }

  override def setJavaMailFolderName(javaMailFolderName: String) {
    super.setJavaMailFolderName(javaMailFolderName)
  }

  override def setJavaMailProviderName(javaMailProviderName: String) {
    super.setJavaMailProviderName(javaMailProviderName)
  }

  override def isOpenReadOnly = super.isOpenReadOnly

  override def setFetchAll(fetchAll: Boolean) {
    super.setFetchAll(fetchAll)
  }

  override def setFetchTaskName(fetchTaskName: String) {
    super.setFetchTaskName(fetchTaskName)
  }

  override def setHost(host: String) {
    super.setHost(host)
  }

  override def setLeave(keep: Boolean) {
    super.setLeave(keep)
  }

  override def setMarkSeen(markSeen: Boolean) {
    super.setMarkSeen(markSeen)
  }

  override def setRecurse(recurse: Boolean) {
    super.setRecurse(recurse)
  }

  override def setLogger(logger: Logger) {
    super.setLogger(logger)
  }

  override def setLocalUsers(localUsers: UsersRepository) {
    super.setLocalUsers(localUsers)
  }

  override def setDNSServer(dnsServer: DNSService) {
    super.setDNSServer(dnsServer)
  }

  override def setLeaveBlacklisted(keepRejected: Boolean) {
    super.setLeaveBlacklisted(keepRejected)
  }

  override def setMarkBlacklistedSeen(markRejectedSeen: Boolean) {
    super.setMarkBlacklistedSeen(markRejectedSeen)
  }

  override def setBlacklist(blacklist: util.Set[MailAddress]) {
    super.setBlacklist(blacklist)
  }

  override def setBlacklist(blacklistValue: String) {
    super.setBlacklist(blacklistValue)
  }

  override def setRejectUserUndefined(localRecipientsOnly: Boolean) {
    super.setRejectUserUndefined(localRecipientsOnly)
  }

  override def setMarkUserUndefinedSeen(markExternalSeen: Boolean) {
    super.setMarkUserUndefinedSeen(markExternalSeen)
  }

  override def setLeaveUserUndefined(leaveExternal: Boolean) {
    super.setLeaveUserUndefined(leaveExternal)
  }

  override def setMarkRemoteRecipientSeen(markRemoteRecipientSeen: Boolean) {
    super.setMarkRemoteRecipientSeen(markRemoteRecipientSeen)
  }

  override def setLeaveRemoteRecipient(leaveRemoteRecipient: Boolean) {
    super.setLeaveRemoteRecipient(leaveRemoteRecipient)
  }

  override def setRejectRemoteRecipient(rejectRemoteRecipient: Boolean) {
    super.setRejectRemoteRecipient(rejectRemoteRecipient)
  }

  override def setDefaultDomainName(defaultDomainName: String) {
    super.setDefaultDomainName(defaultDomainName)
  }

  override def setLeaveUndeliverable(leaveUndeliverable: Boolean) {
    super.setLeaveUndeliverable(leaveUndeliverable)
  }

  override def setMarkUndeliverableSeen(markUndeliverableSeen: Boolean) {
    super.setMarkUndeliverableSeen(markUndeliverableSeen)
  }

  override def setRejectBlacklisted(rejectBlacklisted: Boolean) {
    super.setRejectBlacklisted(rejectBlacklisted)
  }

  override def setLeaveRecipientNotFound(leaveRecipientNotFound: Boolean) {
    super.setLeaveRecipientNotFound(leaveRecipientNotFound)
  }

  override def setMarkRecipientNotFoundSeen(markRecipientNotFoundSeen: Boolean) {
    super.setMarkRecipientNotFoundSeen(markRecipientNotFoundSeen)
  }

  override def setRejectRecipientNotFound(rejectRecipientNotFound: Boolean) {
    super.setRejectRecipientNotFound(rejectRecipientNotFound)
  }

  override def setRemoteReceivedHeaderIndex(remoteReceivedHeaderIndex: Int) {
    super.setRemoteReceivedHeaderIndex(remoteReceivedHeaderIndex)
  }

  override def setDeferRecipientNotFound(deferRecipientNotFound: Boolean) {
    super.setDeferRecipientNotFound(deferRecipientNotFound)
  }

  override def setLeaveMaxMessageSizeExceeded(leaveMaxMessageSize: Boolean) {
    super.setLeaveMaxMessageSizeExceeded(leaveMaxMessageSize)
  }

  override def setMarkMaxMessageSizeExceededSeen(markMaxMessageSizeSeen: Boolean) {
    super.setMarkMaxMessageSizeExceededSeen(markMaxMessageSizeSeen)
  }

  override def setMaxMessageSizeLimit(maxMessageSizeLimit: Int) {
    super.setMaxMessageSizeLimit(maxMessageSizeLimit)
  }

  override def setRejectMaxMessageSizeExceeded(rejectMaxMessageSize: Boolean) {
    super.setRejectMaxMessageSizeExceeded(rejectMaxMessageSize)
  }

  override def setLeaveRemoteReceivedHeaderInvalid(leaveRemoteReceivedHeaderInvalid: Boolean) {
    super.setLeaveRemoteReceivedHeaderInvalid(leaveRemoteReceivedHeaderInvalid)
  }

  override def setMarkRemoteReceivedHeaderInvalidSeen(markRemoteReceivedHeaderInvalidSeen: Boolean) {
    super.setMarkRemoteReceivedHeaderInvalidSeen(markRemoteReceivedHeaderInvalidSeen)
  }

  override def setRejectRemoteReceivedHeaderInvalid(rejectRemoteReceivedHeaderInvalid: Boolean) {
    super.setRejectRemoteReceivedHeaderInvalid(rejectRemoteReceivedHeaderInvalid)
  }
}
