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

package org.eknet.publet.james.mailets

import org.apache.jsieve.mailet.ResourceLocator
import org.eknet.publet.gitr.partition.GitPartition
import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import org.apache.james.user.api.UsersRepository
import java.io.{InputStream, FileNotFoundException}
import org.eknet.publet.james.data.MailDb
import org.eknet.publet.vfs.{ChangeInfo, Writeable}

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.12.12 17:54
 */
@Singleton
class SieveScriptLocator @Inject() (@Named("james-sieve-scripts") gp: GitPartition, userRepo: UsersRepository, maildb: MailDb) extends ResourceLocator {

  private val UriRegex = "//([^@/]+)@([^@/]+)/sieve".r

  def get(uri: String) = {
    val (user, domain) =  uri match {
      case UriRegex(u, d) => (u, d)
      case _ => sys.error("Invalid sieve uri: "+ uri)
    }
    if (!maildb.sieveEnabled(user)) {
      throw new FileNotFoundException("Sieve disabled")
    } else {
      val username = if (userRepo.supportVirtualHosting()) user+"@"+domain else user
      val resource = lookupScript(username)
        .getOrElse(throw new FileNotFoundException("No script found for "+username))

      resource.inputStream
    }
  }

  def lookupScript(login: String) = lookup(login+".sieve").orElse(lookup(login+".siv"))

  def saveScript(login: String, script: InputStream, changeInfo: Option[ChangeInfo]) {
    gp.content(login+".sieve") match {
      case wr: Writeable => wr.writeFrom(script, changeInfo)
      case r@_ => sys.error("Resource not writeable: "+ r)
    }
  }
  private def lookup(name: String) = {
    val x = gp.content(name)
    if (x.exists) Some(x) else None
  }
}
