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

import org.eknet.publet.quartz.QuartzJob
import org.quartz.JobExecutionContext
import com.google.inject.Inject
import org.eknet.publet.web.{Config, Settings}
import org.eknet.publet.Publet
import org.eknet.publet.vfs.fs.FilesystemPartition
import com.google.inject.name.Named
import org.eknet.publet.engine.scalate.ScalateEngine
import org.eknet.publet.vfs._
import grizzled.slf4j.Logging
import org.eknet.publet.web.util.{PubletWebContext, PubletWeb}
import java.text.{SimpleDateFormat, DateFormat}
import scala.Left
import scala.Right
import annotation.tailrec
import java.util.concurrent.atomic.AtomicInteger
import java.util.Locale
import org.eknet.publet.web.asset.AssetManager
import org.eknet.publet.web.template.{IncludeLoader, ConfiguredScalateEngine}
import org.fusesource.scalate.layout.NullLayoutStrategy
import org.fusesource.scalate.Binding

/**
 * This job will generate the james report for the current time and
 * stores it in some partition. Old reports are deleted, if the configured
 * number of reports to preserve is exceeded.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 02.02.13 10:44
 */
final class ReportJob @Inject() (reportMan: ReportJobMBean, writer: ReportWriter) extends QuartzJob with Logging {

  def perform(context: JobExecutionContext) {
    writer.writeReport() match {
      case Left(msg) => error(msg)
      case Right(c) => {
        reportMan.resetAll()
        info("Report "+ c.name +" successfully written.")
      }
    }
    removeReports(reportMan.getKeepReports)
  }

  private[this] def removeReports(keep: Int): Int = {
    val counter = new AtomicInteger(0)
    delete(writer.listReports, reportMan.getKeepReports, counter)
    info("Removed "+ counter.get() +" old reports")
    counter.get()
  }

  @tailrec
  private def delete(reports:List[ContentResource], keep: Int, counter: AtomicInteger) {
    reports match {
      case (a:Modifyable)::as if (as.size >= keep) => {
        a.delete()
        counter.incrementAndGet()
        delete(as, keep, counter)
      }
      case _ =>
    }
  }
}
