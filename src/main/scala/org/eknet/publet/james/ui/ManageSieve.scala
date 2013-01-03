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
import org.eknet.publet.web.shiro.Security
import org.eknet.publet.web.util.{PubletWebContext, PubletWeb}
import org.eknet.publet.vfs.Content
import org.eknet.publet.james.mailets.SieveScriptLocator
import org.apache.james.user.api.UsersRepository
import org.eknet.publet.james.Permissions

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 30.12.12 14:33
 */
class ManageSieve extends ScalaScript {
  import org.eknet.publet.web.util.RenderUtils.makeJson

  def serve() = {
    paramLc(actionParam) match {
      case Some("get") => getScript
      case Some("update") => updateScript()
      case Some("getlogins") => getLogins
      case _ => failure("Unknown command")
    }
  }

  private def updateScript() = safeCall {
    val login = getLogin
    withPerm(Permissions.sieveUpdate(login)) {
      val scriptModified = getSieveScript(login).flatMap(_.lastModification).getOrElse(-1L)
      val lastMod = param("lastHead").map(_.toLong).getOrElse(-1L)
      param("script") match {
        case Some(script) => {
          if (scriptModified <= lastMod) {
            val active = boolParam("active").getOrElse(false)
            maildb.setSieveEnabled(login, active)
            val info = changeInfo("Updates sieve script for "+login)
            getLocator.saveScript(login, Content(script, sieve).inputStream, Some(info))
            makeJson(Map("success"->true,
              "message" -> "Sieve Script saved.",
              "lastHead" -> getSieveScript(login).flatMap(_.lastModification).getOrElse(-1L))
            )
          } else {
            failure("The script has been modified. Please reload the page.")
          }
        }
        case _ => failure("No script given.")
      }
    }
  }

  private def getScript = safeCall {
    val login = getLogin
    withPerm(Permissions.sieveGet(login)) {
      val resource = getSieveScript(login)
      val script = resource.map(_.contentAsString()).getOrElse("")
      val lastMod = resource.flatMap(_.lastModification).map(_.toString).getOrElse(0L)
      makeJson(Map("login" -> login, "script" -> script, "active" -> isActive(login), "lastHead" -> lastMod))
    }
  }

  private def getLogins = withPerm(Permissions.sieveManage) {
    import collection.JavaConversions._
    val repo = PubletWeb.instance[UsersRepository].get
    makeJson(Map("users" -> repo.list().toList))
  }

  private def getSieveScript(login: String) = getLocator.lookupScript(login)

  private def isActive(login: String) = maildb.sieveEnabled(login)

  private def getLocator = PubletWeb.instance[SieveScriptLocator].get

  private def getLogin = {
    val login = PubletWebContext.param("username").map(_.trim).getOrElse(Security.username)
    val repo = PubletWeb.instance[UsersRepository].get
    if (!repo.contains(login)) {
      sys.error("User '"+login+"' not found.")
    } else {
      login
    }
  }
}
