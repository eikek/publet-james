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

import com.google.inject.{Scopes, TypeLiteral, AbstractModule}
import data.{PubletDomainList, RecipientTable, UserRepository}
import guice._
import org.eknet.publet.web.guice.{PubletModule, PubletBinding}
import org.apache.james.smtpserver.netty.SMTPServerFactory
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.dnsservice.dnsjava.DNSJavaService
import org.apache.james.protocols.lib.handler.ProtocolHandlerLoader
import org.apache.james.filesystem.api.FileSystem
import com.google.inject.matcher.{AbstractMatcher, Matchers, Matcher}
import com.google.inject.spi.{TypeEncounter, TypeListener}
import org.apache.james.user.api.UsersRepository
import org.apache.james.rrt.api.RecipientRewriteTable
import org.apache.james.domainlist.api.DomainList
import org.apache.james.queue.api.MailQueueFactory
import org.apache.james.queue.file.FileMailQueueFactory

class PubletJamesModule extends AbstractModule with PubletBinding with PubletModule {

  object JamesMatcher extends AbstractMatcher[TypeLiteral[_]] {
    private val jamesPackage = Matchers.inSubpackage("org.apache.james")
    private val eknetJamesPackage = Matchers.inSubpackage("org.eknet.publet.james")

    def matches(t: TypeLiteral[_]) =
      jamesPackage.matches(t.getRawType) || eknetJamesPackage.matches(t.getRawType)

  }

  def configure() {
    bind(classOf[JamesConfigurationProvider])
    bindListener(JamesMatcher, new JamesTypeListener)

    binder.set[DNSService].toType[DNSJavaService]
    binder.set[ProtocolHandlerLoader].toType[GuiceProtocolHandlerLoader]
    binder.set[FileSystem].toType[PubletFilesystem]

    binder.set[UsersRepository].toType[UserRepository]
    binder.set[RecipientRewriteTable].toType[RecipientTable]
    binder.set[DomainList].toType[PubletDomainList]
    binder.set[MailQueueFactory].toType[FileMailQueueFactory] in Scopes.SINGLETON

    binder.bindEagerly[SMTPServerFactory]

  }

}
