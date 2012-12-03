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
import org.eknet.publet.james.data._
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
import org.apache.james.rrt.lib.RecipientRewriteTableManagement
import org.apache.james.domainlist.lib.DomainListManagement
import org.eknet.publet.james.guice.test.TestUserStore
import org.eknet.guice.squire.SquireModule
import org.eknet.publet.ext.graphdb.GraphDbProvider
import org.eknet.publet.auth.store.UserStore

class PubletJamesModule extends SquireModule with PubletBinding with PubletModule {

  def configure() {
    bind[Setup].asEagerSingleton()
    bind[ConfigurationProvider].to[JamesConfigurationProvider] in  Scopes.SINGLETON
    bindListener(JamesMatcher, new JamesTypeListener)
    bind[PreDestroyHandler].asEagerSingleton()

    bind[DNSService].to[DNSJavaService] in Scopes.SINGLETON
    bind[ProtocolHandlerLoader].to[GuiceProtocolHandlerLoader] in Scopes.SINGLETON
    bind[FileSystem].to[PubletFilesystemImpl] in Scopes.SINGLETON

    bind[UsersRepository].to[UserRepository] in Scopes.SINGLETON
    bind[RecipientRewriteTable].to[RecipientTable] in Scopes.SINGLETON
    bind[DomainList].to[PubletDomainList] in Scopes.SINGLETON

    bind[MailQueueFactory].to[FileMailQueueFactory] in Scopes.SINGLETON

    bind[SMTPServerFactory].asEagerSingleton()

    //mailet container
    bind[MailProcessor].to[CamelCompositeProcessor] in Scopes.SINGLETON
    bind[MailetContext].to[JamesMailetContext].asEagerSingleton()
    bind[JamesMailSpooler].asEagerSingleton()
    bind[MailetLoader].to[MailetMatcherLoader] in Scopes.SINGLETON
    bind[MatcherLoader].to[MailetMatcherLoader] in Scopes.SINGLETON
    bind[CamelContext].to[GuiceCamelContext].asEagerSingleton()

    //maildir
    bind[MailboxPathLocker].to[JVMMailboxPathLocker] in Scopes.SINGLETON
    bind[Authenticator].to[UserRepositoryAuthenticator] in Scopes.SINGLETON
    bind[MailboxACLResolver].to[UnionMailboxACLResolver] in Scopes.SINGLETON
    bind[GroupMembershipResolver].to[SimpleGroupMembershipResolver] in Scopes.SINGLETON
    bind[MaildirStore].to[GMaildirStore] asEagerSingleton()
    bind[MailboxSessionIdGenerator].to[RandomMailboxSessionIdGenerator] in Scopes.SINGLETON
    bind[MaildirMailboxSessionMapperFactory].to[GMaildirMailboxSessionMapperFactory] in Scopes.SINGLETON
    // bean "maildir-mailboxmanager" aliased to "mailboxmanager" by config file
    bind[MailboxManager].to[GStoreMailboxManager] asEagerSingleton()
    // bean "maildir-subscriptionmanager" aliased to "subscriptionmanager" by config file
    bind[SubscriptionManager].to[GStoreSubscriptionManager] asEagerSingleton()
    bind[MailRepositoryStore].to[MailRepositoryStoreImpl] asEagerSingleton()
    bind[MailboxManagerManagement]
    bind[RecipientRewriteTableManagement].asEagerSingleton()
    bind[DomainListManagement].asEagerSingleton()

    //imap
    bind[ImapDecoderFactory].to[DefaultImapDecoderFactory] in Scopes.SINGLETON
    bind[ImapEncoderFactory].to[DefaultImapEncoderFactory] in Scopes.SINGLETON
    bind[IMAPServerFactory].asEagerSingleton()
    bind[MailboxCopier].to[MailboxCopierImpl] in Scopes.SINGLETON

    ///test
    setOf[UserStore].add[TestUserStore].in(Scopes.SINGLETON)
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

  @Provides@Singleton
  def createDb(dbprov: GraphDbProvider): MailDb = new MailDb(dbprov.getDatabase("jamesdb"))
}
