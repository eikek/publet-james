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

package org.eknet.publet.james.ui

import org.eknet.publet.engine.scala.ScalaScript
import org.eknet.publet.web.util.PubletWeb
import org.eknet.publet.james.stats.ReportWriter
import org.eknet.publet.vfs.{ResourceName, ContentResource}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 12.02.13 22:42
 */
class ReportList extends ScalaScript {
  import org.eknet.publet.web.util.RenderUtils.makeJson

  def serve() = {
    paramLc(actionParam) match {
      case Some("getnext") => getNextReports
      case cmd@_ => failure("Unknown command: "+ cmd)
    }
  }

  def getNextReports = {
    param("report") match {
      case Some(r) => {
        findNext(reportWriter.listReports, ResourceName(r).name) match {
          case (Some(prev), Some(next)) => makeJson( makePrevMap(prev) ++ makeNextMap(next) )
          case (None, Some(next)) => makeJson(makeNextMap(next))
          case (Some(prev), None) => makeJson(makePrevMap(prev))
          case _ => makeJson(Map())
        }
      }
      case _ => failure("No report given.")
    }
  }

  def makePrevMap(r: ContentResource) = Map(
    "prevReport" -> r.name.withExtension("html").fullName,
    "prevReportName" -> r.name.name
  )
  def makeNextMap(r: ContentResource) = Map(
    "nextReport" -> r.name.withExtension("html").fullName,
    "nextReportName" -> r.name.name
  )

  def findNext(list: List[ContentResource], current: String): (Option[ContentResource], Option[ContentResource]) = {
    list match {
      case a1::a2::_ if (a1.name.name == current) => (None, Some(a2))
      case a1::a2::a3::_ if (a2.name.name == current) => (Some(a1), Some(a3))
      case a1::a2::Nil if (a2.name.name == current) => (Some(a1), None)
      case a::as => findNext(as, current)
      case _ => (None, None)
    }
  }

  def reportWriter = PubletWeb.instance[ReportWriter].get
}
