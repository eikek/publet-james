package org.eknet.publet.james.server

import org.apache.james.imapserver.netty.IMAPServerFactory
import org.slf4j.Logger
import org.apache.commons.configuration.HierarchicalConfiguration
import com.google.inject.{Inject, Singleton}
import org.apache.james.filesystem.api.FileSystem

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 06.12.12 13:22
 */
@Singleton
class PubletImapServerFactory @Inject() (fs: FileSystem) extends IMAPServerFactory {
  import collection.JavaConversions._

  override def createServers(log: Logger, config: HierarchicalConfiguration) = {
    val configs = config.configurationsAt("imapserver").toSeq.asInstanceOf[Seq[HierarchicalConfiguration]]
    createCertificate(configs, fs)
    super.createServers(log, config)
  }
}
