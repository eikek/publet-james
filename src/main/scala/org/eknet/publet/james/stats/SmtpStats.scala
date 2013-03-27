package org.eknet.publet.james.stats

import org.eknet.county.County

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 10.01.13 15:17
 */
class SmtpStats(tree: County) extends SimpleStats("smtp", tree) {

  def getLocalDeliverySize = getCount(SmtpStats.Keys.localDeliverySize)
  def getRemoteDeliverySize = getCount(SmtpStats.Keys.remoteDeliverySize)
  def getSizeOfAll = getLocalDeliverySize + getRemoteDeliverySize

}

object SmtpStats {

  object Keys extends Enumeration {
    type Key = Value

    implicit def enumToString(value: Enumeration#Value): String = value.toString

    val acceptedMails,
        connections,
        blockedConnections,
        unknownUser,
        relayDenied,
        localDelivery,
        localDeliverySize,
        remoteDeliverySize,
        remoteDelivery = Value
  }
}