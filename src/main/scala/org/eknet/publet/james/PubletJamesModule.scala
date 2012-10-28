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
import org.eknet.publet.james.data.{MailRepositoryStoreImpl, PubletDomainList, RecipientTable, UserRepository}
import guice._
import org.eknet.publet.web.guice.{PubletModule, PubletBinding}
import org.apache.james.smtpserver.netty.SMTPServerFactory
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.dnsservice.dnsjava.DNSJavaService
import org.apache.james.protocols.lib.handler.ProtocolHandlerLoader
import org.apache.james.filesystem.api.FileSystem
import org.apache.james.user.api.UsersRepository
import org.apache.james.rrt.api.RecipientRewriteTable
import org.apache.james.domainlist.api.DomainList
import org.apache.james.queue.api.MailQueueFactory
import org.apache.james.queue.file.FileMailQueueFactory
import org.apache.james.mailetcontainer.impl.camel.CamelCompositeProcessor
import org.apache.mailet.MailetContext
import org.apache.james.mailetcontainer.impl.{JamesMailSpooler, JamesMailetContext}
import org.apache.james.mailetcontainer.api.{MailProcessor, MatcherLoader, MailetLoader}
import org.apache.camel.CamelContext
import org.apache.james.mailbox._
import org.apache.james.mailbox.store._
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
import org.apache.james.mailbox.copier.{MailboxCopierImpl, MailboxCopier}
import org.apache.james.mailrepository.api.MailRepositoryStore
import org.apache.james.adapter.mailbox.MailboxManagerManagement

class PubletJamesModule extends AbstractModule with PubletBinding with PubletModule {

  def configure() {
    binder.set[ConfigurationProvider].toType[JamesConfigurationProvider] in  Scopes.SINGLETON
    bindListener(JamesMatcher, new JamesTypeListener)
    bindListener(MBeanMatcher, new MBeanExporter)
    binder.bindEagerly[PreDestroyHandler]

    binder.set[DNSService].toType[DNSJavaService] in Scopes.SINGLETON
    binder.set[ProtocolHandlerLoader].toType[GuiceProtocolHandlerLoader] in Scopes.SINGLETON
    binder.set[FileSystem].toType[PubletFilesystemImpl] in Scopes.SINGLETON

    binder.set[UsersRepository].toType[UserRepository] in Scopes.SINGLETON
    binder.set[RecipientRewriteTable].toType[RecipientTable] in Scopes.SINGLETON
    binder.set[DomainList].toType[PubletDomainList] in Scopes.SINGLETON

    binder.set[MailQueueFactory].toType[FileMailQueueFactory] in Scopes.SINGLETON

    binder.bindEagerly[SMTPServerFactory]

    //mailet container
    binder.set[MailProcessor].toType[CamelCompositeProcessor] in Scopes.SINGLETON
    binder.set[MailetContext].toType[JamesMailetContext] asEagerSingleton()
    binder.bindEagerly[JamesMailSpooler]
    binder.set[MailetLoader].toType[MailetMatcherLoader] in Scopes.SINGLETON
    binder.set[MatcherLoader].toType[MailetMatcherLoader] in Scopes.SINGLETON
    bind(classOf[CamelContext]).toType[GuiceCamelContext] asEagerSingleton()

    //maildir
    binder.set[MailboxPathLocker].toType[JVMMailboxPathLocker] in Scopes.SINGLETON
    binder.set[Authenticator].toType[UserRepositoryAuthenticator] in Scopes.SINGLETON
    binder.set[MailboxACLResolver].toType[UnionMailboxACLResolver] in Scopes.SINGLETON
    binder.set[GroupMembershipResolver].toType[SimpleGroupMembershipResolver] in Scopes.SINGLETON
    binder.set[MaildirStore].toType[GMaildirStore] asEagerSingleton()
    binder.set[MailboxSessionIdGenerator].toType[RandomMailboxSessionIdGenerator] in Scopes.SINGLETON
    binder.set[MaildirMailboxSessionMapperFactory].toType[GMaildirMailboxSessionMapperFactory] in Scopes.SINGLETON
    // bean "maildir-mailboxmanager" aliased to "mailboxmanager" by config file
    binder.set[MailboxManager].toType[GStoreMailboxManager] asEagerSingleton()
    // bean "maildir-subscriptionmanager" aliased to "subscriptionmanager" by config file
    binder.set[SubscriptionManager].toType[GStoreSubscriptionManager] asEagerSingleton()
    binder.set[MailRepositoryStore].toType[MailRepositoryStoreImpl] asEagerSingleton()
    binder.bindEagerly[MailboxManagerManagement]

    //imap
    binder.set[ImapDecoderFactory].toType[DefaultImapDecoderFactory] in Scopes.SINGLETON
    binder.set[ImapEncoderFactory].toType[DefaultImapEncoderFactory] in Scopes.SINGLETON
    binder.bindEagerly[IMAPServerFactory]
    binder.set[MailboxCopier].toType[MailboxCopierImpl] in Scopes.SINGLETON
  }


  //imap

  @Provides@Singleton
  def createDecoder(decfac: ImapDecoderFactory): ImapDecoder = decfac.buildImapDecoder()

  @Provides@Singleton
  def createEncode(encfac: ImapEncoderFactory): ImapEncoder = encfac.buildImapEncoder()

  @Provides@Singleton
  def createImapProcessor(boxman: MailboxManager, subman: SubscriptionManager): ImapProcessor = {
    import collection.JavaConversions._
    DefaultImapProcessorFactory.createXListSupportingProcessor(boxman, subman, null, 120L, Set("ACL"))
  }
}
