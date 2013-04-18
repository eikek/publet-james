/*
 * Copyright 2013 Eike Kettner
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

package org.eknet.publet.james.maildir

import lib.Maildir
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox
import org.apache.james.mailbox.model.MailboxPath

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 14.01.13 20:31
 */
class MaildirMailbox[Id](val maildir: Maildir, val path: MailboxPath) extends SimpleMailbox[Id](path, maildir.uidlist.getUidValidity) {

}
