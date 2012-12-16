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
import org.eknet.publet.web.util.RenderUtils.makeJson

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 16.12.12 13:42
 */
class ManageAlias extends ScalaScript {

  def serve() = {
    if (!Security.isAuthenticated) {
      failure("Not authenticated.")
    } else {
      paramLc(actionParam) match {
        case Some("add") => addAlias()
        case Some("delete") => removeAlias()
        case Some("getalias") => getAliases
        case _ => failure("Unknown command.")
      }
    }
  }

  private def checkLocalDomain = {
    paramLc("domain").collect({
      case d if (domainList.getDefaultDomain==d || domainList.getDomains.contains(d)) => d
    })
  }

  private def removeAlias() = {
    val alias = paramLc("alias").map(_.split("@", 2)).map(a => a(0)->a(1))
    val user = alias.map(_._1)
    val domain = alias.map(_._2)
    (user, domain) match {
      case (Some(u), Some(d)) => safeCall {
        val login = Security.username
        maildb.removeMapping(u, d, login)
        success("Alias removed.")
      }
      case _ => failure("Too less paramters.")
    }
  }

  private def addAlias() = {
    val user = paramLc("user")
    val domain = checkLocalDomain
    (user, domain) match {
      case (Some(u), Some(d)) => safeCall {
        if (maildb.aliasExists(u, d)) {
          failure("This alias does already exist.")
        } else {
          val login = Security.username
          maildb.addMapping(u, d, login)
          success("Alias added.")
        }
      }
      case _ => failure("Too less paramters.")
    }
  }

  private def getAliases = {
    val login = Security.username
    val aliases = maildb.allMappings.collect({ case t if (t._2.size == 1 && t._2.head == login) => t._1 })
    makeJson(aliases.toList)
  }
}
