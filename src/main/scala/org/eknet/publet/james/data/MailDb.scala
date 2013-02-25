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

package org.eknet.publet.james.data

import org.eknet.scue.GraphDsl
import com.google.inject.{Inject, Singleton}
import org.apache.james.rrt.api.RecipientRewriteTable
import grizzled.slf4j.Logging
import org.eknet.publet.ext.graphdb.GraphDb
import com.google.inject.name.Named
import org.eknet.publet.james.fetchmail.{Account, FetchmailConfig}
import java.util.concurrent.TimeUnit
import org.apache.james.fetchmail.{AccountConfiguration, ConfiguredAccount}
import com.tinkerpop.blueprints.Vertex
import java.util.concurrent.atomic.AtomicInteger
import javax.mail.Session
import java.util.Properties
import org.apache.james.user.api.UsersRepository

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 28.10.12 20:00
 */
@Singleton
class MailDb @Inject() (@Named("jamesdb") val db: GraphDb, userRepo: UsersRepository) extends Logging {

  private implicit val graph = db.graph
  import GraphDsl._

  private[this] def domains = withTx(vertex("name" := "domainNames"))
  private[this] def virtualAddr =  withTx(vertex("name" := "virtualAddress"))
  private[this] def mappings = withTx(vertex("name" := "allMappings"))

  private val domainNameProp = "domainName"
  private val domainNameLabel = "domain"

  def addDomain(domain: String) {
    if (domain == null || domain.isEmpty)
      throw new IllegalArgumentException("Cannot add empty domain names!")

    withTx {
      vertex(domainNameProp := domain.toLowerCase, v => domains --> domainNameLabel --> v)
    }
  }

  def removeDomain(domain: String) {
    if (domain == null || domain.isEmpty)
      throw new IllegalArgumentException("Cannot remove empty domain names!")

    withTx {
      vertices(domainNameProp := domain) foreach (dv => {
        info("Remove domain vertex: "+ dv)
        graph.removeVertex(dv)
      })
    }
  }

  def containsDomain(domain: String): Boolean = withTx {
    if (domain == null || domain.isEmpty)
      throw new IllegalArgumentException("Empty domain names are not allowed!")

    vertices(domainNameProp := domain).headOption.isDefined
  }

  def getDomainList: List[String] = withTx {
    (domains ->- domainNameLabel mapEnds (v => v.get[String](domainNameProp)
      .collect({case x if (!x.isEmpty) => x}))).flatten.toList.distinct
  }

  /*
     Mapping outline
   */

  private val mappingLabel = "mapping"
  private val mappingProp = "mappingName"
  private val vaddressProp = "virtualAddress"
  private val addressLabel = "address"

  private def key(user: String, domain: String) =
    Option(user).getOrElse(RecipientRewriteTable.WILDCARD) +
    "@" + Option(domain).getOrElse(RecipientRewriteTable.WILDCARD)

  def addMapping(user: String, domain: String, mapping: String) {
    withTx {
      vertex(vaddressProp := key(user, domain), v => {
        virtualAddr --> addressLabel --> v
      }) --> mappingLabel -->| (newVertex+=(mappingProp := mapping)) <-- mappingLabel <-- mappings
    }
  }

  def removeMapping(user: String, domain: String, mapping: String) {
    withTx {
      vertices(vaddressProp := key(user, domain)).headOption.map(v => {
        (v ->- mappingLabel findEnd(_.has(mappingProp := mapping))).foreach(graph.removeVertex(_))
      })
    }
    removeUserDomainNodeIfEmpty(user, domain)
  }


  private[this] def removeUserDomainNodeIfEmpty(user: String, domain: String) {
    withTx {
      if (userDomainMappings(user, domain) == Nil) {
        vertices(vaddressProp := key(user, domain)).headOption.map(v => graph.removeVertex(v))
      }
    }
  }

  def removeAllMappings(user: String, domain: String) {
    withTx {
      vertices(vaddressProp := key(user, domain)).headOption.map(v => {
        v ->- mappingLabel mapEnds(mn => graph.removeVertex(mn))
      })
    }
    removeUserDomainNodeIfEmpty(user, domain)
  }

  def userDomainMappings(user: String, domain: String) = withTx {
    vertices(vaddressProp := key(user, domain)).headOption.map(v => {
      (v ->- mappingLabel mapEnds(mn => mn.get[String](mappingProp))).flatten
    }).getOrElse(Nil)
  }

  def allMappings = withTx {
    (virtualAddr ->- addressLabel mapEnds {van => (van.get[String](vaddressProp).get ->
      (van ->- mappingLabel mapEnds(_.get[String](mappingProp).get)).toList) }).toMap
  }

  /**
   * Returns whether there exists a recipient with the given name. That is,
   * either there is a local user with this login, or there exists a mapping
   * for the given user and domain already.
   *
   * @param user
   * @param domain
   * @return
   */
  def aliasExists(user: String, domain: String): Boolean = {
    if (userRepo.contains(user)) {
      true
    } else {
      allMappings.find(m => {
        val mapping = m._1.split("@", 2)
        mapping(0) == user && mapping(1) == domain
      }).isDefined
    }
  }

  // ~~ fetchmail

  def fetchmailConfig = withTx {
      val v = vertex("fetchmailConfig" := 0, v => v.setProperty("enabled", false))
      new FetchmailConfig(v.get[Boolean]("enabled").getOrElse(false),
        v.get[Long]("interval").getOrElse(10),
        v.get[String]("unit").map(s => TimeUnit.valueOf(s.toUpperCase)).getOrElse(TimeUnit.MINUTES),
        v.get[Long]("run").getOrElse(0L)
      )
    }

  def updateFetchmailConfig(cfg: FetchmailConfig) {
    withTx {
      val v = vertex("fetchmailConfig" := 0, v => v.setProperty("enabled", false))
      v("enabled") = cfg.enabled
      v("interval") = cfg.interval
      v("unit") = cfg.unit.name()
      v("run") = cfg.run
    }
  }

  /**
   * Returns accounts for the specified run. The objects are not yet configured,
   * the [[org.apache.james.fetchmail.ConfiguredAccount.configure()]] method must
   * be called for each account.
   *
   * @param run
   * @param localDomain
   * @return
   */
  def getAccountsForRun(run: Long, localDomain: String): Iterable[ConfiguredAccount] = {
    def runFilter(v: Vertex) =
      v.get[Boolean]("active").exists(_ == true) && (run % v.get[Int]("runInterval").getOrElse(2) == 0)

    val hostPort = "([^:]+):([\\d]+)".r
    val counter = new AtomicInteger(0)
    val session = Session.getInstance(new Properties(System.getProperties))
    withTx {
      vertices("type" := "fetchmailAccount").withFilter(runFilter).map { v =>
        val cfg = new AccountConfiguration()
        val acc = vertexToAccount(v)
        cfg.setJavaMailProviderName(
          if (acc.ssl) "pop3s" else "pop3"
        )
        acc.host match {
          case hostPort(h, p) => {
            cfg.setHost(h)
            session.getProperties.setProperty("mail."+cfg.getJavaMailProviderName+".port", p)
          }
          case _ => cfg.setHost(acc.host)
        }
        cfg.setRejectRemoteRecipient(true)
        cfg.setMarkSeen(false)

        cfg.setLeaveMaxMessageSizeExceeded(true)
        session.getProperties.setProperty("mail.pop3.ssl.enable", java.lang.Boolean.toString(acc.ssl))
        if (!acc.ssl) {
          session.getProperties.setProperty("mail.pop3.starttls.enable", "true")
        }

        val account = new ConfiguredAccount()
        account.setSequenceNumber(counter.getAndIncrement)
        account.setSession(session)
        account.setUser(acc.user)
        account.setPassword(acc.password)
        account.setRecipient(acc.login +"@"+ localDomain)
        account.setParsedConfiguration(cfg)
        account.setCustomRecipientHeader("")
        account.setIgnoreRecipientHeader(true)
        account
      }
    }
  }

  /**
   * Adds or update the given account. Accounts are identified
   * by the string "user@host". Accounts are connected to a
   * node that represents the login.
   *
   * @param account
   */
  def updateAccount(account: Account) {
    withTx {
      val key = account.user +"@"+ account.host
      val v = vertex("fetchmailAccount" := key, v => {
        v("type") = "fetchmailAccount"
        val userv = vertex("login" := account.login)
        userv --> "fetchmailAccount" --> v
      })
      v("host") = account.host
      v("ssl") = account.ssl
      v("user") = account.user
      v("recipient") = account.login
      v("password") = account.password
      v("runInterval") = account.runInterval
      v("active") = account.active
    }
  }

  private[this] def vertexToAccount(v: Vertex) = Account(
    v.get[String]("recipient").getOrElse(sys.error("no login found")),
    v.get[String]("host").getOrElse(sys.error("no host found")),
    v.get[Boolean]("ssl").getOrElse(false),
    v.get[String]("user").getOrElse(sys.error("no remote user found")),
    v.get[String]("password").getOrElse(sys.error("no password found")),
    v.get[Int]("runInterval").getOrElse(2),
    v.get[Boolean]("active").getOrElse(true)
  )

  def findAccount(user: String, host: String) = {
    val key = user +"@"+ host
    withTx {
      vertices("fetchmailAccount" := key).headOption.map(vertexToAccount)
    }
  }

  def deleteAccount(user: String, host: String) {
    val key = user +"@"+ host
    withTx {
      vertices("fetchmailAccount" := key).map(v => {
        graph.removeVertex(v)
      })
    }
  }

  def getAccountsForLogin(login: String): Iterable[Account] = {
    withTx {
      val loginv = vertices("login" := login).headOption
      loginv.map(v => v ->- "fetchmailAccount" mapEnds(vertexToAccount)).getOrElse(Nil)
    }
  }

  // sieve scripts

  def sieveEnabled(login: String): Boolean = withTx {
    val v = vertex("login" := login)
    v.get[Boolean]("sieveEnabled").getOrElse(false)
  }

  def setSieveEnabled(login: String, enabled: Boolean) {
    withTx {
      val v = vertex("login" := login)
      v("sieveEnabled") = enabled
    }
  }
}
