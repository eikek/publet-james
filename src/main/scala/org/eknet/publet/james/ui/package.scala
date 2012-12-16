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

package org.eknet.publet.james

import org.eknet.publet.engine.scala.ScalaScript
import org.eknet.publet.web.util.{RenderUtils, PubletWebContext, PubletWeb}
import org.apache.james.domainlist.api.DomainList
import org.eknet.publet.vfs.Content
import org.eknet.publet.james.data.MailDb

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 01.11.12 12:00
 */
package object ui {

  val actionParam = "do"

  def param(name: String) = PubletWebContext.param(name).filter(!_.trim.isEmpty).map(_.toLowerCase)
  def intParam(name: String) = param(name) map (s => s.toInt)
  def boolParam(name: String) = param(name) map (s => s match {
    case "on" | "ON" | "On" => true
    case "true" | "TRUE" | "True" => true
    case "yes" | "YES" | "Yes" => true
    case _ => false
  })
  def domainList = PubletWeb.instance[DomainList].get
  def maildb = PubletWeb.instance[MailDb].get

  def success(msg: String) = RenderUtils.makeJson(Map("success"->true, "message"->msg))
  def failure(msg: String) = RenderUtils.makeJson(Map("success"->false, "message"->msg))

  def safeCall(f: => Option[Content]) = try {
    f
  } catch {
    case e: Exception => failure(e.getMessage)
  }

}
