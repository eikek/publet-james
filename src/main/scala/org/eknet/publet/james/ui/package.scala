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

  def domainList = PubletWeb.instance[DomainList].get
  def maildb = PubletWeb.instance[MailDb].get
  def param(name: String) = PubletWebContext.param(name)

  def success(msg: String) = RenderUtils.makeJson(Map("success"->true, "message"->msg))
  def failure(msg: String) = RenderUtils.makeJson(Map("success"->false, "message"->msg))

  def safeCall(f: => Option[Content]) = try { f } catch {
      case e: Exception => failure(e.getMessage)
    }

  class GetDomains extends ScalaScript {
    def serve() = RenderUtils.makeJson((domainList.getDefaultDomain :: maildb.getDomainList).distinct)
  }

  class GetDefaultDomain extends ScalaScript {
    def serve() = RenderUtils.makeJson(domainList.getDefaultDomain)
  }

  class AddDomain extends ScalaScript {
    def serve() = {
      param("domain") match {
        case Some(d) => safeCall {
          domainList.addDomain(d)
          success("Domain '"+d+"' created")
        }
        case _ => failure("No domain specified.")
      }
    }
  }

  class RemoveDomain extends ScalaScript {
    def serve() = {
      param("domain") match {
        case Some(d) => safeCall {
          maildb.removeDomain(d)
          success("Domain '"+d+"' removed.")
        }
        case _ => failure("No domain specified.")
      }
    }
  }

  class GetMappings extends ScalaScript {
    def serve() = {
      RenderUtils.makeJson(maildb.allMappings)
    }
  }

  class AddMapping extends ScalaScript {
    def serve() = {
      val user = param("user").filter(!_.trim.isEmpty)
      val domain = param("domain").filter(!_.trim.isEmpty)
      val mapping = param("mapping").filter(!_.trim.isEmpty)

      (user, domain, mapping) match {
        case (u, d, Some(m)) if (d != None || u != None) => {
          maildb.addMapping(u.getOrElse("*"), d.getOrElse("*"), m)
          success("Mapping added.")
        }
        case _ => failure("Too less parameters.")
      }
    }
  }
}