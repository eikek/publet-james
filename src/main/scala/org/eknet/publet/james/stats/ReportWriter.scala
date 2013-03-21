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

import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import org.eknet.publet.vfs.{Resource, Path, Writeable, ContentResource}
import org.eknet.publet.vfs.fs.FilesystemPartition
import org.eknet.publet.web.template.{IncludeLoader, ConfiguredScalateEngine}
import org.fusesource.scalate.layout.NullLayoutStrategy
import org.eknet.publet.Publet
import org.eknet.publet.web.Config
import org.eknet.publet.web.asset.AssetManager
import java.util.Locale
import java.text.SimpleDateFormat
import org.eknet.publet.james.server.ConnectionBlacklist

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 02.02.13 15:26
 */
@Singleton
class ReportWriter @Inject() (publet: Publet,
                              config: Config,
                              smtpStats: SmtpStatsService,
                              assetMgr: AssetManager,
                              blacklist: ConnectionBlacklist,
                              @Named("imap") imapStats: LoginStatsService,
                              @Named("pop3") pop3Stats: LoginStatsService,
                              @Named("james-reports") reportPart: FilesystemPartition) {


  private val reportPath = Path("/publet/james/_report.jade")
  private lazy val engine = createTemplateEngine

  private lazy val attributes = Map(
    "locale" -> Locale.getDefault,
    "smtpStats" -> smtpStats,
    "pop3Stats" -> pop3Stats,
    "imapStats" -> imapStats,
    "blackList" -> blacklist,
    "includeLoader" -> new IncludeLoader(config, publet, assetMgr)
  )

  private[this] def createTemplateEngine = {
    val te = new ConfiguredScalateEngine('dummy, publet, config, assetMgr)
    te.engine.combinedClassPath = true
    te.engine.layoutStrategy = NullLayoutStrategy
    te
  }

  private def createReportName() = {
    val now = new java.util.Date(System.currentTimeMillis())
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm")
    dateFormat.format(now) + "-report.ssp"
  }

  def writeReport(): Either[String, ContentResource] = writeReport(createReportName())

  def writeReport(name: String): Either[String, ContentResource] = {
    val cnt = engine.processUri(reportPath.asString, None, attributes)
    val report = reportPart.content(name)
    report match {
      case w: Writeable => {
        cnt.copyTo(w.outputStream)
        Right(report)
      }
      case _ => Left("The report partition is not writable: "+ reportPart)
    }
  }

  private[this] val resourceComparator = (r1: Resource, r2: Resource) => {
      val lm1 = r1.lastModification.getOrElse(0L)
      val lm2 = r2.lastModification.getOrElse(0L)
      lm1.compareTo(lm2) < 0
  }

  def listReports = reportPart.children
    .collect({case c: ContentResource => c})
    .toList
    .sortWith(resourceComparator)

}
