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
import org.eknet.publet.web.util.RenderUtils

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 04.12.12 21:11
 */
class ManageDomains extends ScalaScript {
  import org.eknet.publet.web.util.PubletWebContext.param

  def serve() = {
    param(actionParam) match {
      case Some("get") => getDomains
      case Some("add") => addDomain()
      case Some("remove") => removeDomain()
      case Some("getdefault") => getDefaultDomain
      case cmd @_ => failure("Unknown command: "+ cmd)
    }
  }

  def getDomains = RenderUtils.makeJson((domainList.getDefaultDomain :: maildb.getDomainList).distinct)

  def getDefaultDomain = RenderUtils.makeJson(domainList.getDefaultDomain)

  def addDomain() = param("domain") match {
    case Some(d) => safeCall {
      domainList.addDomain(d)
      success("Domain '"+d+"' created")
    }
    case _ => failure("No domain specified.")
  }

  def removeDomain() = param("domain") match {
    case Some(d) => safeCall {
      maildb.removeDomain(d)
      success("Domain '"+d+"' removed.")
    }
    case _ => failure("No domain specified.")
  }

}
