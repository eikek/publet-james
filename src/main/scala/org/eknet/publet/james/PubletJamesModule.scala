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

import com.google.inject._
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
import org.apache.james.mailetcontainer.impl.camel.CamelCompositeProcessor
import org.apache.mailet.MailetContext
import org.apache.james.mailetcontainer.impl.{JamesMailSpooler, JamesMailetContext}
import org.apache.james.mailetcontainer.api.{MatcherLoader, MailetLoader}
import org.apache.camel.CamelContext
import org.apache.james.mailbox.{SubscriptionManager, MailboxPathLocker}
import org.apache.james.mailbox.store.{StoreSubscriptionManager, StoreMailboxManager, Authenticator, JVMMailboxPathLocker}
import org.apache.james.adapter.mailbox.store.UserRepositoryAuthenticator
import org.apache.james.mailbox.maildir.{MaildirMailboxSessionMapperFactory, MaildirStore}
import org.apache.james.mailbox.acl.{SimpleGroupMembershipResolver, GroupMembershipResolver, UnionMailboxACLResolver, MailboxACLResolver}
import org.apache.james.mailbox.store.user.SubscriptionMapperFactory
import org.apache.james.imap.decode.{ImapDecoder, ImapDecoderFactory}
import org.apache.james.imap.main.DefaultImapDecoderFactory
import org.apache.james.imap.encode.{ImapEncoder, ImapEncoderFactory}
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory
import org.apache.james.imapserver.netty.IMAPServerFactory
import org.apache.james.imap.api.process.ImapProcessor
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory

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

    //mailet container
    bind(classOf[CamelCompositeProcessor])
    binder.set[MailetContext].toType[JamesMailetContext]
    bind(classOf[JamesMailSpooler])
    binder.set[MailetLoader].toType[GuiceMailetLoader] //todo merge same code of those two classes
    binder.set[MatcherLoader].toType[GuiceMatcherLoader]
    bind(classOf[CamelContext]).toType[GuiceCamelContext] asEagerSingleton()

    //maildir
    binder.set[MailboxPathLocker].toType[JVMMailboxPathLocker]
    binder.set[Authenticator].toType[UserRepositoryAuthenticator]
    binder.set[MailboxACLResolver].toType[UnionMailboxACLResolver]
    binder.set[GroupMembershipResolver].toType[SimpleGroupMembershipResolver]

    //imap
    binder.set[ImapDecoderFactory].toType[DefaultImapDecoderFactory]
    binder.set[ImapEncoderFactory].toType[DefaultImapEncoderFactory]
    binder.bindEagerly[IMAPServerFactory]
  }

  //maildir
  @Provides@Singleton
  def createMaildirStore(locker:MailboxPathLocker): MaildirStore = new MaildirStore("var/mailboxes/%domain/%user", locker)

  @Provides@Singleton
  def createMaildirMapperFactory(maildirStore: MaildirStore): MaildirMailboxSessionMapperFactory
    = new MaildirMailboxSessionMapperFactory(maildirStore)

  // bean "maildir-mailboxmanager" aliased to "mailboxmanager" by config file
  @Provides@Singleton
  def createStoreMailboxMan(fac: MaildirMailboxSessionMapperFactory,
                            auth: Authenticator,
                            locker:MailboxPathLocker,
                            acl:MailboxACLResolver,
                            group:GroupMembershipResolver): StoreMailboxManager[java.lang.Integer] =
    new StoreMailboxManager(fac, auth, locker, acl, group)

  // bean "maildir-subscriptionmanager" aliased to "subscriptionmanager" by config file
  @Singleton@Provides
  def createSubscriptionMan(fac: MaildirMailboxSessionMapperFactory): SubscriptionManager =
    new StoreSubscriptionManager(fac)

  //imap

  @Provides@Singleton
  def createDecoder(decfac: ImapDecoderFactory): ImapDecoder = decfac.buildImapDecoder()

  @Provides@Singleton
  def createEncode(encfac: ImapEncoderFactory): ImapEncoder = encfac.buildImapEncoder()

  @Provides@Singleton
  def createImapProcessor(boxman: StoreMailboxManager[java.lang.Integer], subman: SubscriptionManager): ImapProcessor = {
    import collection.JavaConversions._
    DefaultImapProcessorFactory.createXListSupportingProcessor(boxman, subman, null, 120L, Set("ACL"))
  }
}
