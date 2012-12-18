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

package org.eknet.publet.james.ui

import org.eknet.publet.engine.scala.ScalaScript
import org.eknet.publet.web.util.PubletWeb
import org.eknet.publet.james.fetchmail.FetchmailScheduler

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 16.12.12 02:39
 */
class ManageFetchmailScheduler extends ScalaScript {

  import org.eknet.publet.web.util.RenderUtils.makeJson

  def serve() = {
    val fm = PubletWeb.instance[FetchmailScheduler].get
    paramLc(actionParam) match {
      case Some("get") => withPerm("james:fetchmail:scheduler:get") {
        makeJson(Map(
          "startedLabel" -> ( if (fm.isScheduled) "success" else "important" ),
          "schedulerState" -> (if (fm.isScheduled) "Running" else "Stopped"),
          "interval" -> fm.getInterval,
          "action" -> (if (fm.isScheduled) "stop" else "play")
        ))
      }
      case Some("play") => withPerm("james:fetchmail:scheduler:start") {
        fm.start()
        success("Fetchmail started")
      }
      case Some("stop") => withPerm("james:fetchmail:scheduler:stop") {
        fm.stop()
        success("Fetchmail stopped")
      }
      case Some("set") => withPerm("james:fetchmail:scheduler:set") {
        intParam("interval") match {
          case Some(interval) => safeCall {
            fm.setInterval(interval)
            success("Interval updated to "+ interval +" minutes.")
          }
          case _ => failure("Interval missing.")
        }
      }
      case _ => failure("Too less parameters.")
    }
  }

}
