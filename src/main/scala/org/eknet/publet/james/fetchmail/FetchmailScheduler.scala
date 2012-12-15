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

import com.google.inject.{Inject, Singleton}
import org.quartz.{TriggerKey, JobKey, Scheduler}
import org.eknet.publet.james.data.MailDb
import org.eknet.publet.quartz.QuartzDsl
import org.eknet.publet.web.guice.PubletStartedEvent
import com.google.common.eventbus.Subscribe
import java.util.concurrent.TimeUnit

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 16:57
 */
@Singleton
class FetchmailScheduler @Inject() (scheduler: Scheduler, maildb: MailDb) extends FetchmailSchedulerMBean with QuartzDsl {

  private val jobKey: JobKey = "fetchmail0" in "fetchmail"
  private val triggerKey: TriggerKey = "fetchmailTrigger" in "fetchmail"

  private val fetchmailJob = newJob[FetchmailJob]
    .withIdentity(jobKey)
    .withDescription("Fetches external mail and delivers it locally.")
    .build()

  @Subscribe
  def autoStart(event: PubletStartedEvent) {
    val config = maildb.fetchmailConfig
    update(config)
  }

  def start() {
    val config = maildb.fetchmailConfig.copy(enabled = true, run = 0L)
    update(config)
  }

  def stop() {
    val config = maildb.fetchmailConfig.copy(enabled = false)
    update(config)
  }

  def isScheduled = scheduler.checkExists(triggerKey)

  def setInterval(minutes: Int) {
    val config = maildb.fetchmailConfig.copy(interval = minutes, unit = TimeUnit.MINUTES)
    update(config)
  }

  def getInterval = {
    val config = maildb.fetchmailConfig
    config.unit.toMinutes(config.interval).toInt
  }

  def update(config: FetchmailConfig) {
    if (config.enabled) {
      if (scheduler.checkExists(triggerKey)) {
        scheduler.rescheduleJob(triggerKey, createTrigger(config))
      } else {
        scheduler.scheduleJob(fetchmailJob, createTrigger(config))
      }
    } else {
      scheduler.unscheduleJob(triggerKey)
    }
    maildb.updateFetchmailConfig(config)
  }

  private[this] def createTrigger(cfg: FetchmailConfig) = {
    newTrigger
      .withIdentity(triggerKey).forJob(jobKey)
      .withSchedule(simpleSchedule
        .withIntervalInMilliseconds(cfg.unit.toMillis(cfg.interval))
        .repeatForever())
      .build()
  }
}
