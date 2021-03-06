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

import com.google.inject.{Inject, Singleton}
import maildir.lib.PathLock
import maildir.{MaildirStore, MaildirSessionMapperFactory}
import org.eknet.publet.web.Config
import java.nio.file.Path
import org.apache.james.mailbox.store.{StoreSubscriptionManager, StoreMailboxManager, Authenticator, MailboxSessionMapperFactory}
import org.apache.james.mailbox.acl.{MailboxACLResolver, GroupMembershipResolver}
import org.apache.james.mailbox.{MailboxSessionIdGenerator, MailboxPathLocker}

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 24.10.12 20:04
 */
package object guice {

  @Singleton
  class GMaildirStore @Inject()
    (config: Config, locker: PathLock[Path]) extends MaildirStore(config.workDir("james/inboxes/%user").getAbsolutePath, locker)

  @Singleton
  class GStoreMailboxManager @Inject() (fac: MaildirSessionMapperFactory,
          auth: Authenticator,
          locker:MailboxPathLocker,
          acl:MailboxACLResolver,
          group:GroupMembershipResolver,
          idgen: MailboxSessionIdGenerator) extends StoreMailboxManager[Int](fac, auth, locker, acl, group) {

    setMailboxSessionIdGenerator(idgen)
    //misses @PostConstruct annotation
    init()
  }

  @Singleton
  class GStoreSubscriptionManager @Inject() (fac: MaildirSessionMapperFactory) extends StoreSubscriptionManager(fac)
}
