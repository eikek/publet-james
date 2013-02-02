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

import javax.management.MXBean

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 02.02.13 13:20
 */
@MXBean
trait ReportJobMBean {

  def resetAll()

  def resetSmtp()
  def resetImap()
  def resetPop3()

  def isReportWritingEnabled: Boolean
  def getKeepReports: Int
  def getCronExpression: String

  def startReportWriting()
  def stopReportWriting()

  def writeReport()

}
