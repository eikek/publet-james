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

import org.apache.james.mailbox.store.user.SubscriptionMapper
import org.apache.james.mailbox.store.user.model.Subscription
import java.nio.file.Files
import java.nio.charset.Charset
import org.apache.james.mailbox.store.user.model.impl.SimpleSubscription

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 04.02.13 22:30
 */
class MaildirSubscriptionMapper(store: MaildirStore) extends SubscriptionMapper with NoTransaction {

  private val fileName = "publet-james-subscriptions"

  def findMailboxSubscriptionForUser(user: String, mailbox: String) = {
    val subscriptions = readSubscriptions(user)
    subscriptions.find(_ == mailbox).map(m => new SimpleSubscription(user, mailbox)).orNull
  }

  def save(subscription: Subscription) {
    import collection.JavaConversions._
    val subscriptions = readSubscriptions(subscription.getUser)
    if (!subscriptions.contains(subscription.getMailbox)) {
      Files.write(subscriptionFile(
        subscription.getUser),
        subscription.getMailbox :: subscriptions,
        Charset.defaultCharset())
    }
  }

  def findSubscriptionsForUser(user: String) = {
    import collection.JavaConversions._
    readSubscriptions(user).map(s => new SimpleSubscription(user, s))
  }

  def delete(subscription: Subscription) {
    import collection.JavaConversions._
    val subscriptions = readSubscriptions(subscription.getUser)
    if (subscriptions.contains(subscription.getMailbox)) {
      Files.write(subscriptionFile(
        subscription.getUser),
        subscriptions.filter(_ != subscription.getMailbox),
        Charset.defaultCharset())
    }
  }

  private def readSubscriptions(user: String) = {
    import collection.JavaConversions._
    Files.readAllLines(subscriptionFile(user), Charset.defaultCharset()).toList
  }

  private def subscriptionFile(user: String) = store.getInbox(user).folder.resolve(fileName)

  def endRequest() {
  }
}
