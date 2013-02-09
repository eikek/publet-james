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

package org.eknet.publet.james.stats

import org.eknet.publet.quartz.QuartzDsl
import org.quartz.{TriggerKey, Scheduler, JobKey}
import org.eknet.publet.web.guice.PubletStartedEvent
import com.google.inject.{Singleton, Inject}
import com.google.common.eventbus.Subscribe
import org.eknet.publet.web.{SettingsReloadedEvent, Settings}
import com.google.inject.name.Named

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 02.02.13 12:50
 */
@Singleton
class ReportJobScheduler @Inject() (scheduler: Scheduler,
                                    settings: Settings,
                                    @Named("imap") imapStats: LoginStatsService,
                                    @Named("pop3") pop3Stats: LoginStatsService,
                                    smtpStats: SmtpStatsService) extends ReportJobMBean with QuartzDsl {

  private val defaultCronExpression = "0 0 4 * * ?"
  private val reportJobTrigger: TriggerKey = "reportWriterTrigger" in "james-report"
  private val reportJobKey: JobKey = "reportWriter" in "james-report"

  private val reportJob = newJob[ReportJob]
    .withIdentity(reportJobKey)
    .withDescription("Writes reports periodically to the file system")
    .build()

  @Subscribe
  def autoStart(ev: PubletStartedEvent) {
    toggleReportWriting()
  }

  @Subscribe
  def settingsReload(ev: SettingsReloadedEvent) {
    toggleReportWriting()
  }

  private def toggleReportWriting() {
    if (jamesReportEnabled) {
      startReportWriting()
    } else {
      stopReportWriting()
    }
  }

  private def jamesReportEnabled = settings("james.report.enabled").getOrElse("true").toBoolean

  private def createTrigger = newTrigger
    .withIdentity(reportJobTrigger).forJob(reportJobKey)
    .withSchedule(cronScheduler(getCronExpression))
    .build()

  def getKeepReports = settings("james.report.keep").getOrElse("10").toInt

  def getCronExpression = settings("james.report.cronTrigger").getOrElse(defaultCronExpression)

  def isReportWritingEnabled = scheduler.checkExists(reportJobTrigger)

  def startReportWriting() {
    if (scheduler.checkExists(reportJobTrigger)) {
      scheduler.rescheduleJob(reportJobTrigger, createTrigger)
    } else {
      scheduler.scheduleJob(reportJob, createTrigger)
    }
  }

  def stopReportWriting() {
    if (scheduler.checkExists(reportJobTrigger)) {
      scheduler.unscheduleJob(reportJobTrigger)
    }
  }

  def writeReport() {
    scheduler.triggerJob(reportJobKey)
  }

  def resetAll() {
    resetImap()
    resetPop3()
    resetSmtp()
  }

  def resetSmtp() {
    smtpStats.reset()
  }

  def resetImap() {
    imapStats.reset()
  }

  def resetPop3() {
    pop3Stats.reset()
  }
}
