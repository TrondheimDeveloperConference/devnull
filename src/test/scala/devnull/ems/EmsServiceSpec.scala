package devnull.ems

import java.util.UUID

import devnull.ems.helpers.UtcLocalDateTime
import org.scalatest.{FunSpec, Matchers}

class EmsServiceSpec extends FunSpec with Matchers {

  import scala.language.reflectiveCalls

  class EmsServiceInTest(ei: List[EventInfo]) extends EmsService {
    override def eventInfos: List[EventInfo] = ei
  }

  trait Fixtures {
    val event = Event(UUID.randomUUID(), "Some Event")
    val sessionId = UUID.randomUUID()
    val now = UtcLocalDateTime.now()
  }

  describe("valid") {
    it("when after end time") {
      new Fixtures {
        val ts = Some(TimeSlot(now.minusHours(2), now.minusMinutes(1)))
        val service: EmsServiceInTest = new EmsServiceInTest(List(EventInfo(event, sessionId, ts)))

        service.isValid(sessionId) shouldBe true
      }
    }

  }

  describe("invalid") {
    it("when before end time") {
      new Fixtures {
        val ts = Some(TimeSlot(now.minusHours(2), now.plusMinutes(1)))
        val service: EmsServiceInTest = new EmsServiceInTest(List(EventInfo(event, sessionId, ts)))

        service.isValid(sessionId) shouldBe false
      }

    }

    it("when no matching session id") {
      new Fixtures {
        val ts = Some(TimeSlot(now.minusHours(2), now.plusMinutes(1)))
        val service: EmsServiceInTest = new EmsServiceInTest(List(EventInfo(event, sessionId, ts)))

        service.isValid(UUID.randomUUID()) shouldBe false
      }
    }

    it("when event does not have time slot") {
      new Fixtures {
        val service: EmsServiceInTest = new EmsServiceInTest(List(EventInfo(event, sessionId, None)))

        service.isValid(sessionId) shouldBe false
      }
    }

  }

}
