package org.eknet.publet.james.ui

import org.eknet.publet.engine.scala.ScalaScript
import org.eknet.publet.web.util.{RenderUtils, PubletWeb}
import org.eknet.publet.james.server.{PubletPop3ServerFactory, PubletImapServerFactory, PubletSmtpServerFactory}
import org.apache.james.protocols.lib.netty.{AbstractServerFactory, AbstractConfigurableAsyncServer}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 15.12.12 01:07
 */
class ManageServer extends ScalaScript {
  import collection.JavaConversions._

  def serve() = {
    (param(actionParam), param("serverType")) match {
      case (Some("get"), Some(stype)) => safeCall {
        val servers = getServer(stype)
        val maps = servers.zip(0 to servers.size).map(s => Map(
          "serverType" -> stype,
          "secure" -> (s._1.getSocketType=="secure" || s._1.getStartTLSSupported),
          "serverState" -> (if (s._1.isStarted) "Running" else "Stopped"),
          "startedLabel" -> (if (s._1.isStarted) "success" else "important"),
          "boundAddress" -> s._1.getBoundAddresses.mkString(", "),
          "currentConnections" -> s._1.getCurrentConnections,
          "action" -> (if (s._1.isStarted) "stop" else "play"),
          "index" -> s._2
        ))
        RenderUtils.makeJson(maps)
      }
      case (Some("stop"), Some(stype)) => {
        param("index") match {
          case Some(num) => safeCall {
            val server = getServer(stype)(num.toInt)
            if (server.isStarted) server.stop()
            success("Server stopped")
          }
          case _ => failure("Unkown server to operate on.")
        }
      }
      case (Some("play"), Some(stype)) => {
        param("index") match {
          case Some(num) => safeCall {
            val server = getServer(stype)(num.toInt)
            if (!server.isStarted) server.start()
            success("Server started")
          }
          case _ => failure("Unkown server to operate on.")
        }
      }
      case cmd @_ => failure("Unknown command: "+ cmd)
    }
  }

  private[this] def getServer(stype: String): List[AbstractConfigurableAsyncServer] = {
    val factory = stype match {
      case "smtp" => PubletWeb.instance[PubletSmtpServerFactory].get
      case "imap" =>  PubletWeb.instance[PubletImapServerFactory].get
      case "pop3" =>  PubletWeb.instance[PubletPop3ServerFactory].get
      case _ => sys.error("Unknown server type: "+ stype)
    }
    factory.getServers.toList
  }

}
