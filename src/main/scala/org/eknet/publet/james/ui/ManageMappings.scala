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
 * @since 04.12.12 21:16
 */
class ManageMappings extends ScalaScript {

  def serve() = param(actionParam) match {
    case Some("get") => getMappings
    case Some("add") => addMapping()
    case Some("remove") => removeMapping()
    case cmd @_ => failure("Unknown command: "+ cmd)
  }

  def getMappings = param("q") match {
    case Some(q) => RenderUtils.makeJson(maildb.allMappings.filter(t => {
      t._1.contains(q) || t._2.exists(s => s.contains(q))
    }))
    case _ => RenderUtils.makeJson(maildb.allMappings)
  }

  def addMapping() = {
    val user = param("user")
    val domain = param("domain")
    val mapping = param("mapping")

    (user, domain, mapping) match {
      case (u, d, Some(m)) if (d != None || u != None) => {
        maildb.addMapping(u.getOrElse("*"), d.getOrElse("*"), m)
        success("Mapping added.")
      }
      case _ => failure("Too less parameters.")
    }
  }

  def removeMapping() = {
    val user = param("user")
    val domain = param("domain")
    val mapping = param("mapping")
    (user, domain, mapping) match {
      case (Some(u), Some(d), Some(m)) => {
        maildb.removeMapping(u, d, m)
        success("Mapping removed.")
      }
      case (Some(u), Some(d), None) => {
        maildb.removeAllMappings(u, d)
        success("Mapping(s) removed.")
      }
      case _ => failure("Too less parameters.")
    }
  }
}
