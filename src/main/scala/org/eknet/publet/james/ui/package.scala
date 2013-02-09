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

import org.eknet.publet.web.util.{RenderUtils, PubletWebContext, PubletWeb}
import org.apache.james.domainlist.api.DomainList
import org.eknet.publet.vfs.{ChangeInfo, ContentType, Content}
import org.eknet.publet.james.data.MailDb
import org.eknet.publet.web.shiro.Security
import org.apache.shiro.authz.{UnauthorizedException, UnauthenticatedException}
import org.apache.shiro.authz
import org.eknet.publet.auth.store.UserProperty
import org.eknet.publet.web.{Settings, RunMode, Config}
import grizzled.slf4j.Logging

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 01.11.12 12:00
 */
package object ui extends Logging {

  val sieve = ContentType.sieve

  val actionParam = "do"

  def param(name: String) = PubletWebContext.param(name).filter(!_.trim.isEmpty)
  def paramLc(name: String) = param(name).map(_.toLowerCase)
  def intParam(name: String) = param(name) map (s => s.toInt)
  def boolParam(name: String) = param(name) map (s => s match {
    case "on" | "ON" | "On" => true
    case "true" | "TRUE" | "True" => true
    case "yes" | "YES" | "Yes" => true
    case _ => false
  })
  def domainList = PubletWeb.instance[DomainList].get
  def maildb = PubletWeb.instance[MailDb].get
  def config = PubletWeb.instance[Config].get
  def settings = PubletWeb.instance[Settings].get

  def success(msg: String) = RenderUtils.makeJson(Map("success"->true, "message"->msg))
  def failure(msg: String) = RenderUtils.makeJson(Map("success"->false, "message"->msg))

  def safeCall(f: => Option[Content]) = try {
    f
  } catch {
    case e: UnauthorizedException => failure("Permission denied.")
    case e: Exception => {
      debug("Error executing script.", e)
      failure(e.getMessage)
    }
  }

  def authenticated(f: => Option[Content]) = {
    if (Security.hasGroup(Permissions.mailgroup)) f else failure("Not authenticated.")
  }

  def withPerm(perm: String)(b: => Option[Content]) = safeCall {
    if (!Security.isAuthenticated) throw new UnauthenticatedException("Not authenticated.")
    if (!Security.hasGroup(Permissions.mailgroup)) throw new authz.UnauthenticatedException()
    Security.checkPerm(perm)
    b
  }

  def changeInfo(message: String) = ChangeInfo(
    Some(Security.username),
    Security.user.flatMap(_.get(UserProperty.email)),
    message
  )
}
