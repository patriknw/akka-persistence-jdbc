/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.persistence.jdbc.query

import scala.concurrent.duration._

import akka.pattern.ask
import akka.persistence.query.EventEnvelope
import akka.persistence.query.NoOffset
import akka.persistence.query.Sequence
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory

object SpannerEventsByTagTest {
  val maxBufferSize = 20
  val refreshInterval = 500.milliseconds

  val configOverrides: Map[String, ConfigValue] = Map(
    "jdbc-read-journal.max-buffer-size" -> ConfigValueFactory.fromAnyRef(maxBufferSize.toString),
    "jdbc-read-journal.refresh-interval" -> ConfigValueFactory.fromAnyRef(refreshInterval.toString()))
}

class SpannerEventsByTagTest
    extends QueryTestSpec("spanner-shared-db-application.conf", SpannerEventsByTagTest.configOverrides)
    with SpannerCleaner {
  final val NoMsgTime: FiniteDuration = 100.millis

  def assertEnvelope(a: EventEnvelope, b: EventEnvelope): Unit = {
    a.event shouldBe b.event
    a.persistenceId shouldBe b.persistenceId
    a.sequenceNr shouldBe b.sequenceNr
  }

  // FIXME more tests from EventsByTagTest are needed but this is the basic query

  it should "not find events for unknown tags" in withActorSystem { implicit system =>
    val journalOps = new ScalaJdbcReadJournalOperations(system)
    withTestActors() { (actor1, actor2, actor3) =>
      actor1 ! withTags(1, "one")
      actor2 ! withTags(2, "two")
      actor3 ! withTags(3, "three")

      eventually {
        journalOps.countJournal.futureValue shouldBe 3
      }

      journalOps.withEventsByTag()("unknown", NoOffset) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNoMessage(NoMsgTime)
        tp.cancel()
      }
    }
  }

  it should "find all events by tag" in withActorSystem { implicit system =>
    val journalOps = new ScalaJdbcReadJournalOperations(system)
    withTestActors(replyToMessages = true) { (actor1, actor2, actor3) =>
      (actor1 ? withTags(1, "number")).futureValue
      (actor2 ? withTags(2, "number")).futureValue
      (actor3 ? withTags(3, "number")).futureValue

      journalOps.withEventsByTag()("number", NoOffset) { tp =>
        tp.request(Int.MaxValue)
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(1), "my-1", 1, 1))
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(2), "my-2", 1, 2))
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(3), "my-3", 1, 3))
        tp.cancel()
      }

      journalOps.withEventsByTag()("number", NoOffset) { tp =>
        tp.request(Int.MaxValue)
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(1), "my-1", 1, 1))
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(2), "my-2", 1, 2))
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(3), "my-3", 1, 3))
        tp.expectNoMessage(NoMsgTime)

        actor1 ? withTags(1, "number")
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(4), "my-1", 2, 1))

        actor1 ? withTags(1, "number")
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(5), "my-1", 3, 1))

        actor1 ? withTags(1, "number")
        assertEnvelope(tp.expectNext(), EventEnvelope(Sequence(6), "my-1", 4, 1))
        tp.cancel()
        tp.expectNoMessage(NoMsgTime)
      }
    }
  }

}

class SpannerScalaEventsByTagTest extends EventsByTagTest("spanner-shared-db-application.conf") with SpannerCleaner
