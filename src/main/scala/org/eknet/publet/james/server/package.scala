package org.eknet.publet.james

import org.apache.commons.configuration.HierarchicalConfiguration
import org.eknet.publet.james.util.MakeCertificate
import org.apache.james.filesystem.api.FileSystem

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 06.12.12 13:23
 */
package object server {

  def createCertificate(configs: Seq[HierarchicalConfiguration], fs: FileSystem) {
    for (config <- configs) {
      val useStartTLS = config.getBoolean("tls.[@startTLS]", false)
      val useSSL = config.getBoolean("tls.[@socketTLS]", false)
      if (useSSL || useStartTLS) {
        val keystore = config.getString("tls.keystore")
        val file = fs.getFile(keystore)
        synchronized {
          if (!file.exists()) {
            file.getParentFile.mkdirs()
            val secret = config.getString("tls.secret", "")
            MakeCertificate.generateSelfSignedCertificate("localhost", file, secret)
          }
        }
      }
    }
  }
}
