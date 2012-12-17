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

import org.eknet.publet.quartz.{QuartzDsl, QuartzJob}
import com.google.inject.Inject
import org.eknet.publet.james.data.MailDb
import org.quartz.{Scheduler, JobExecutionContext}
import grizzled.slf4j.Logging
import org.apache.james.fetchmail.{ConfiguredAccount, FetchmailProcessor}
import org.apache.james.user.api.UsersRepository
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.domainlist.api.DomainList
import org.apache.james.queue.api.MailQueueFactory
import java.util.concurrent.atomic.AtomicInteger
import collection.mutable.ListBuffer
import org.eknet.publet.web.Config

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 16:45
 */
class FetchmailJob @Inject() (config: Config, maildb: MailDb, userRepo: UsersRepository, dns: DNSService, dnl: DomainList, mailqf: MailQueueFactory) extends QuartzJob with Logging with QuartzDsl {

  def perform(context: JobExecutionContext) {
    val fetchmailConfig = maildb.fetchmailConfig
    info("Starting fetchmail run "+ fetchmailConfig.run)
    val queue = mailqf.getQueue(MailQueueFactory.SPOOL)
    val accCount, partCount = new AtomicInteger(0)
    val jobsize = config("james.fetchmail.jobsize").map(_.toInt).getOrElse(10)
    val buff = new ListBuffer[ConfiguredAccount]()
    for (account <- maildb.getAccountsForRun(fetchmailConfig.run, dnl.getDefaultDomain)) {
      account.configure(logger.logger, userRepo, dns, queue, dnl)
      buff += account
      if (buff.length == jobsize) {
        scheduleFetch(partCount.getAndIncrement, buff.toList, context.getScheduler)
        buff.clear()
      }
      accCount.getAndIncrement
    }
    if (!buff.isEmpty) {
      scheduleFetch(partCount.get(), buff.toList, context.getScheduler)
    }
    info("Fetched mail from "+ accCount.get()+ " accounts.")
    maildb.updateFetchmailConfig(fetchmailConfig.copy(run = fetchmailConfig.run +1))
  }

  private[this] def scheduleFetch(count: Int, list: List[ConfiguredAccount], scheduler: Scheduler) {
    val job = newJob[FetchJob].withIdentity("fetch"+count in "fetchmail").build()
    job.getJobDataMap.put("accounts", list)
    val trigger = newTrigger.startNow().build()
    scheduler.scheduleJob(job, trigger)
  }

}
class FetchJob extends QuartzJob {
  def perform(context: JobExecutionContext) {
    val list = context.getJobDetail.getJobDataMap.get("accounts").asInstanceOf[Iterable[ConfiguredAccount]]
    list.foreach(FetchmailProcessor.processAccount)
  }
}
