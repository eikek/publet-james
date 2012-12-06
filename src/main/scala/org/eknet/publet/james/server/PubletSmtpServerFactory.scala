package org.eknet.publet.james.server

import org.apache.james.smtpserver.netty.{SMTPServerFactory, SMTPServer}
import org.apache.commons.configuration.{Configuration, HierarchicalConfiguration}
import org.apache.james.filesystem.api.FileSystem
import org.slf4j.Logger
import com.google.inject.{Inject, Singleton}
import org.eknet.publet.james.util.MakeCertificate
import org.eknet.publet.web.Config

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 06.12.12 12:29
 */
@Singleton
class PubletSmtpServerFactory @Inject() (fs: FileSystem) extends SMTPServerFactory {
  import collection.JavaConversions._

  override def createServers(log: Logger, config: HierarchicalConfiguration) = {
    val configs = config.configurationsAt("smtpserver").toSeq.asInstanceOf[Seq[HierarchicalConfiguration]]
    createCertificate(configs, fs)
    super.createServers(log, config)
  }
}
