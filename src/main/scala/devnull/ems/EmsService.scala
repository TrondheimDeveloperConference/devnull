package devnull.ems

import java.time.LocalDateTime
import java.util.UUID

import devnull.ems.helpers.UtcLocalDateTime

import scala.concurrent.duration.Duration

trait EmsService extends SessionValidation

trait EmsCache extends SessionService {

  def refreshAfter: Duration

  def client: EmsDataFetcher

  var cachedEventInfos: List[EventInfo] = Nil

//  client.fetchEventInfo().onSuccess({case l => update(l)})

  def update(l: List[EventInfo]): Unit = {
    cachedEventInfos = l
//    Future {
//    }
  }

  override def eventInfos: List[EventInfo] = {
    cachedEventInfos
  }

}

trait SessionService {
  def eventInfos: List[EventInfo]
}

trait SessionValidation extends SessionService {

  def isValid(sessionId: UUID): Boolean = {
    val now: LocalDateTime = UtcLocalDateTime.now()

    eventInfos.find(_.sessionId == sessionId).flatMap(_.timeSlot) match {
      case Some(ts) => now.isAfter(ts.end);
      case None => false
    }
  }

}