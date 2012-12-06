package org.eknet.publet.james.server

import org.apache.james.pop3server.netty.POP3ServerFactory
import org.slf4j.Logger
import org.apache.commons.configuration.HierarchicalConfiguration
import com.google.inject.{Singleton, Inject}
import org.apache.james.filesystem.api.FileSystem

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 06.12.12 13:33
 */
@Singleton
class PubletPop3ServerFactory @Inject() (fs: FileSystem) extends POP3ServerFactory {
  import collection.JavaConversions._

  override def createServers(log: Logger, config: HierarchicalConfiguration) = {
    val configs = config.configurationsAt("pop3server").toSeq.asInstanceOf[Seq[HierarchicalConfiguration]]
    createCertificate(configs, fs)
    super.createServers(log, config)
  }
}
