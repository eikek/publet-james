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

package org.eknet.publet.james.guice

import com.google.inject._
import name.{Named, Names}
import org.eknet.publet.james.data._
import org.eknet.publet.web.guice.{AbstractPubletModule, PubletModule, PubletBinding}
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.dnsservice.dnsjava.DNSJavaService
import org.apache.james.protocols.lib.handler.ProtocolHandlerLoader
import org.apache.james.filesystem.api.FileSystem
import org.apache.james.user.api.UsersRepository
import org.apache.james.rrt.api.RecipientRewriteTable
import org.apache.james.domainlist.api.DomainList
import org.apache.james.queue.api.MailQueueFactory
import org.apache.james.mailetcontainer.impl.camel.CamelCompositeProcessor
import org.apache.mailet.MailetContext
import org.apache.james.mailetcontainer.impl.{JamesMailSpooler, JamesMailetContext}
import org.apache.james.mailetcontainer.api.{MailProcessor, MatcherLoader, MailetLoader}
import org.apache.camel.CamelContext
import org.apache.james.mailbox._
import org.apache.james.mailbox.store._
import org.apache.james.adapter.mailbox.store.UserRepositoryAuthenticator
import org.apache.james.mailbox.acl.{SimpleGroupMembershipResolver, GroupMembershipResolver, UnionMailboxACLResolver, MailboxACLResolver}
import org.apache.james.imap.decode.{ImapDecoder, ImapDecoderFactory}
import org.apache.james.imap.main.DefaultImapDecoderFactory
import org.apache.james.imap.encode.{ImapEncoder, ImapEncoderFactory}
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory
import org.apache.james.imap.api.process.ImapProcessor
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory
import org.apache.james.mailrepository.api.MailRepositoryStore
import org.apache.james.adapter.mailbox.MailboxManagerManagement
import org.apache.james.rrt.lib.RecipientRewriteTableManagement
import org.apache.james.domainlist.lib.DomainListManagement
import org.eknet.publet.james.guice.test.TestUserStore
import org.eknet.publet.ext.graphdb.GraphDb
import org.eknet.publet.auth.store.{PermissionStore, UserStore}
import org.eknet.publet.vfs.{Path, Resource}
import org.eknet.publet.vfs.util.SimpleContentResource
import org.apache.james.fetchmail.FetchScheduler
import org.eknet.publet.gitr.partition.{GitPartition, GitPartMan, Config => GitConfig}
import org.apache.jsieve.mailet.{Poster, ResourceLocator}
import com.google.common.eventbus.EventBus
import org.eknet.publet.web.Config
import org.eknet.publet.vfs.fs.FilesystemPartition
import java.nio.file
import org.eknet.publet.james.Reflect
import org.eknet.publet.james.server.{ConnectionBlacklistImpl, ConnectionBlacklist, NotifyingImapProcessor, PubletPop3ServerFactory, PubletImapServerFactory, PubletSmtpServerFactory}
import org.eknet.publet.james.stats.{ReportJobScheduler, ReportJobMBean, Pop3StatsCollector, ImapStatsCollector, LoginStatsService, SmtpStatsCollector, SmtpStatsService}
import org.eknet.publet.james.mailets.{SimpleMailingListHeaders, PubletSieveMailet, MailPoster, SieveScriptLocator}
import org.eknet.publet.james.maildir.lib.{JvmLocker, PathLock}
import org.eknet.publet.james.maildir.{MaildirSessionMapperFactory, MaildirStore, MailboxPathLockerImpl}
import org.eknet.publet.james.fetchmail.{FetchmailAccounts, FetchmailAccountsMBean, FetchmailScheduler}
import org.eknet.county.{Granularity, BasicCounterPool, County}

class PubletJamesModule extends AbstractPubletModule with PubletBinding with PubletModule {

  private[this] def doc(name: String) = {
    val r = Resource.classpath("org/eknet/publet/james/"+ name)
    if (name.endsWith("conf")) {
      new SimpleContentResource(r.name.withExtension("xml"), r)
    } else {
      r
    }
  }

  def configure() {
    bindDocumentation(List(
      doc("doc/james.md"),
      doc("doc/james-sn2.png"),
      doc("doc/james-sn3.png"),
      doc("doc/james-sn4.png"),
      doc("doc/james-sn5.png"),
      doc("doc/james-sn6.png"),
      doc("doc/james-sn7.png"),
      doc("config/domainlist.xml"),
      doc("config/imapserver.xml"),
      doc("config/pop3server.xml"),
      doc("config/smtpserver.xml"),
      doc("config/mailetcontainer.xml"))
    )

    bind[Setup].asEagerSingleton()
    bind[ConfigurationProvider].to[JamesConfigurationProvider] in  Scopes.SINGLETON
    bindListener(JamesMatcher, new JamesTypeListener)
    bind[PreDestroyHandler].asEagerSingleton()

    bind[DNSService].to[DNSJavaService] in Scopes.SINGLETON
    bind[ProtocolHandlerLoader].to[GuiceProtocolHandlerLoader] in Scopes.SINGLETON
    bind[PubletFilesystemImpl]
    bind[FileSystem].to[PubletFilesystemImpl] in Scopes.SINGLETON
    bind[PubletFilesystem].to[PubletFilesystemImpl] in Scopes.SINGLETON

    bind[GraphDb].annotatedWith(Names.named("jamesdb")).toProvider(classOf[DbProvider])
    bind[MailDb].in(Scopes.SINGLETON)

    bind[UsersRepository].to[UserRepository] in Scopes.SINGLETON
    bind[RecipientRewriteTable].to[RecipientTable] in Scopes.SINGLETON
    bind[DomainList].to[PubletDomainList] in Scopes.SINGLETON

    bind[MailQueueFactory].to[FileMailQFactory] in Scopes.SINGLETON

    bind[PubletSmtpServerFactory].asEagerSingleton()
    bind[SmtpStatsService].to[SmtpStatsCollector].asEagerSingleton()

    //mailet container
    bind[MailProcessor].to[CamelCompositeProcessor] in Scopes.SINGLETON
    bind[MailetContext].to[JamesMailetContext].asEagerSingleton()
    bind[JamesMailSpooler].asEagerSingleton()
    bind[MailetLoader].to[MailetMatcherLoader] in Scopes.SINGLETON
    bind[MatcherLoader].to[MailetMatcherLoader] in Scopes.SINGLETON
    bind[CamelContext].to[GuiceCamelContext].asEagerSingleton()

    //sieve/local delivery
    bind[SieveScriptLocator]
    bind[ResourceLocator].to[SieveScriptLocator]
    bind[Poster].to[MailPoster]
    bind[PubletSieveMailet]

    //maildir
    bind[PathLock[file.Path]].to[JvmLocker[file.Path]] in Scopes.SINGLETON
    bind[MailboxPathLocker].to[MailboxPathLockerImpl] in Scopes.SINGLETON

    bind[Authenticator].to[UserRepositoryAuthenticator] in Scopes.SINGLETON
    bind[MailboxACLResolver].to[UnionMailboxACLResolver] in Scopes.SINGLETON
    bind[GroupMembershipResolver].to[SimpleGroupMembershipResolver] in Scopes.SINGLETON
    bind[MaildirStore].to[GMaildirStore] asEagerSingleton()
    bind[MailboxSessionIdGenerator].to[RandomMailboxSessionIdGenerator] in Scopes.SINGLETON
    bind[MaildirSessionMapperFactory] in Scopes.SINGLETON
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
    bind[PubletImapServerFactory].asEagerSingleton()
    bind[LoginStatsService].annotatedWith(Names.named("imap")).to[ImapStatsCollector].asEagerSingleton()

    //pop3
    bind[PubletPop3ServerFactory].asEagerSingleton()
    bind[LoginStatsService].annotatedWith(Names.named("pop3")).to[Pop3StatsCollector].asEagerSingleton()

    //fetchmail
    bind[FetchScheduler].asEagerSingleton()
    bind[FetchmailScheduler].asEagerSingleton()
    bind[FetchmailAccountsMBean].to[FetchmailAccounts].asEagerSingleton()

    //simple mailing list headers
    bind[SimpleMailingListHeaders].asEagerSingleton()

    bind[ReportJobMBean].to[ReportJobScheduler].asEagerSingleton()

    bind[ConnectionBlacklist].to[ConnectionBlacklistImpl].in(Scopes.SINGLETON)

    ///test
    bind[TestUserStore]
    setOf[UserStore].add[TestUserStore].in(Scopes.SINGLETON)
    setOf[PermissionStore].add[TestUserStore].in(Scopes.SINGLETON)
  }

  @Provides@Singleton@Named("connectionCounter")
  def createCounterTree(): County = {
    val c = County.create()
    c.counterFactories = List("**" -> new BasicCounterPool(Granularity.Hour))
    c
  }

  // sieve partition

  @Provides@Singleton@Named("james-sieve-scripts")
  def createSievePartition(gpman: GitPartMan): GitPartition =
    gpman.getOrCreate(Path("james-sieve-scripts"), GitConfig())

  // report partition
  @Provides@Singleton@Named("james-reports")
  def createReportPartition(config: Config, bus: EventBus): FilesystemPartition  = {
    val reportDir = config.workDir("james-reports")
    new FilesystemPartition(reportDir, bus, createDir = true)
  }

  //imap

  @Provides@Singleton
  def createDecoder(decfac: ImapDecoderFactory): ImapDecoder = decfac.buildImapDecoder()

  @Provides@Singleton
  def createEncode(encfac: ImapEncoderFactory): ImapEncoder = encfac.buildImapEncoder()

  @Provides@Singleton
  def createImapProcessor(boxman: MailboxManager, subman: SubscriptionManager, bus: EventBus, blacklist: ConnectionBlacklist): ImapProcessor = {
    import collection.JavaConversions._
    val imapproc = DefaultImapProcessorFactory.createXListSupportingProcessor(boxman, subman, null, 120L, Set("ACL"))
    new NotifyingImapProcessor(bus, blacklist, imapproc)
  }

  val name = "James Mailserver"
  override val version = Reflect.version
  override val license = Reflect.licenses.headOption
}

