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

package org.eknet.publet.james.fetchmail

import org.eknet.publet.quartz.QuartzJob
import com.google.inject.Inject
import org.eknet.publet.james.data.MailDb
import org.quartz.JobExecutionContext
import grizzled.slf4j.Logging
import org.apache.james.fetchmail.FetchmailProcessor
import org.apache.james.user.api.UsersRepository
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.domainlist.api.DomainList
import org.apache.james.queue.api.MailQueueFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 16:45
 */
class FetchmailJob @Inject() (maildb: MailDb, userRepo: UsersRepository, dns: DNSService, dnl: DomainList, mailqf: MailQueueFactory) extends QuartzJob with Logging {

  def perform(context: JobExecutionContext) {
    val config = maildb.fetchmailConfig
    info("Starting fetchmail run "+ config.run)
    val queue = mailqf.getQueue(MailQueueFactory.SPOOL)
    val counter = new AtomicInteger(0)
    for (account <- maildb.getAccountsForRun(config.run, dnl.getDefaultDomain)) {
      account.configure(logger.logger, userRepo, dns, queue, dnl)
      FetchmailProcessor.processAccount(account)
      counter.getAndIncrement
    }
    info("Fetched mail from "+ counter.get()+ " accounts done.")
    maildb.updateFetchmailConfig(config.copy(run = config.run +1))
  }
}
