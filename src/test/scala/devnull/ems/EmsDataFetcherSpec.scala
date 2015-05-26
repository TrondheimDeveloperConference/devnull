package devnull.ems

import java.net.URI
import java.util.UUID

import devnull.ems.helpers.UtcLocalDateTime
import dispatch.Future
import net.hamnaberg.json.collection._
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class EmsDataFetcherSpec extends FunSpec with Matchers {

  import scala.concurrent.ExecutionContext.Implicits.global

  it("should fetch a published event with time slot") {
    trait MockClient extends Client {
      override def client(url: String): Future[JsonCollection] = {
        val m = "(.*)/.*".r
        url match {
          case "base" => Future {
            val eventLinks = List(Link(URI.create("events/" + UUID.randomUUID().toString), "event collection"))
            JsonCollection(URI.create("base"), eventLinks, List())
          }
          case m("events") => Future {
            val sessionUri = URI.create("sessions/" + UUID.randomUUID().toString)
            val eventItem = Item(
              URI.create(url),
              List(Property("name", "JavaZone 2015")),
              List(Link(sessionUri, "session collection")))
            JsonCollection(URI.create("event1"), Nil, List(eventItem))
          }
          case m("sessions") => Future {
            val uri = URI.create(url)
            val prompt = Some("2014-09-10T14:10:00Z+2014-09-10T14:20:00Z")
            val sessionItem = List(
              Item(uri, List(Property("published", true)), List(Link(URI.create("slot1"), "slot item", prompt))),
              Item(uri, List(Property("published", false)), List(Link(URI.create("slot2"), "slot item", prompt))),
              Item(uri, Nil, List(Link(URI.create("slot3"), "slot item", prompt)))
            )
            JsonCollection(URI.create("session1"), Nil, sessionItem)
          }
        }
      }
    }

    val ems: EmsDataFetcher = new EmsDataFetcher("base") with MockClient
    val eventInfos: List[EventInfo] = Await.result(ems.fetchEventInfo(), 10 seconds)

    eventInfos should have size 1
    eventInfos.head.event.name should be("JavaZone 2015")
    eventInfos.head.timeSlot should be(Some(TimeSlot(
      UtcLocalDateTime.parse("2014-09-10T14:10:00Z"),
      UtcLocalDateTime.parse("2014-09-10T14:20:00Z"))))
  }

  it("should fail when the client future cast an exception") {
    trait MockClient extends Client {
      override def client(url: String): Future[JsonCollection] = {
        val m = "(.*)/.*".r
        url match {
          case "base" => Future {
            val eventLinks = List(Link(URI.create("events/" + UUID.randomUUID().toString), "event collection"))
            JsonCollection(URI.create("base"), eventLinks, List())
          }
          case m("events") => Future {
            throw new RuntimeException("Exception in event future")
          }
        }
      }
    }

    val f: Future[List[EventInfo]] = (new EmsDataFetcher("base") with MockClient).fetchEventInfo()
    Await.ready(f, 10 seconds)

    f.value.get.isFailure shouldBe true
  }

}
