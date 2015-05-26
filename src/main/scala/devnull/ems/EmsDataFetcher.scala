package devnull.ems

import java.util.UUID

import dispatch.Defaults._
import dispatch._
import net.hamnaberg.json.collection.Value.StringValue
import net.hamnaberg.json.collection._
import net.hamnaberg.json.collection.data.JavaReflectionData
import JsonCollcetionHelpers._

import scala.language.postfixOps

case class TimeSlot(start: String, end: String)
case class EventInfo(event: Event, sessionId: UUID, timeSlot: Option[TimeSlot])
case class Session(published: Boolean)

case class Event(id: UUID, name: String)
object Event {
  def apply(item: Item): Event = {
    val name = item.data.find(_.name == "name").flatMap(_.asValue.map({case StringValue(v) => v; case _ => ""})).getOrElse("")
    Event(toUuid(item), name)
  }
}

abstract class EmsDataFetcher(emsUrl: String) extends Client{

  implicit val formats = org.json4s.DefaultFormats
  implicit val extractor = new JavaReflectionData[Session]

  def fetchEventInfo(): Future[List[EventInfo]] = {
    for {
      base <- client(emsUrl)
      event <- client(toUrl(base.links, "event collection"))
      sessions <- Future.sequence(event.items.map(item => client(toUrl(item.links, "session collection")).map(ses => (Event(item), ses))))
      eventsInfos <- Future(sessions.flatMap { case (eventId, session) => session.items.filter(isPublished).map(s => itemToEventInfo(s, eventId, toUuid(s))) })
    } yield eventsInfos
  }

  private def itemToEventInfo(item: Item, event: Event, sessionId: UUID): EventInfo = {
    val timeslot = for {
      link <- item.links.find(_.rel == "slot item")
      prompt <- link.prompt
      (startTime, endTime) <- prompt.split('+') match {
        case Array(p1, p2) => Some((p1, p2))
        case _ => None
      }
    } yield TimeSlot(startTime, endTime)
    EventInfo(event, sessionId, timeslot)
  }

  private def isPublished(item: Item): Boolean = {
    item.unapply[Session] match {
      case Some(session) => session.published
      case _ => false
    }
  }
}

object JsonCollcetionHelpers {

  def toUrl(links: List[Link], rel: String): String = {
    links.find(_.rel == rel) match {
      case Some(link) => link.href.toString
      case None => throw new IllegalStateException(s"No rel matches $rel ${links.size}")
    }
  }

  def toUuid(item: Item): UUID = {
    val link = item.href.toString
    UUID.fromString(link.substring(link.length - 36))
  }
}
