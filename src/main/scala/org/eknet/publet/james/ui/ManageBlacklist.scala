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
import org.eknet.publet.web.util.RenderUtils
import org.eknet.publet.james.Permissions
import org.eknet.publet.web.shiro.Security

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 04.12.12 21:11
 */
class ManageBlacklist extends ScalaScript {

  def serve() = {
    paramLc(actionParam) match {
      case Some("get") => getIps
      case Some("add") => addIp()
      case Some("remove") => removeIp()
      case cmd @_ => failure("Unknown command: "+ cmd)
    }
  }

  def getIps = withPerm(Permissions.showBlacklist) {
    RenderUtils.makeJson(Map("ips" -> blackList.listIps))
  }

  def addIp() = paramLc("ip") match {
    case Some(ip) => withPerm(Permissions.modifyBlacklist) {
      blackList.addIp(ip)
      success("Ip '"+ip+"' added")
    }
    case _ => failure("No ip specified.")
  }

  def removeIp() = paramLc("ip") match {
    case Some(ip) => withPerm(Permissions.modifyBlacklist) {
      blackList.removeIp(ip)
      success("Ip '"+ip+"' removed.")
    }
    case _ => failure("No ip specified.")
  }

}
