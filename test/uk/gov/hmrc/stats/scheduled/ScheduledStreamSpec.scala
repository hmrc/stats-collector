/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.scheduled

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.apache.pekko.testkit.{ ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.{ Configuration, Logger }
import uk.gov.hmrc.stats.config.ScheduledJobConfig
import uk.gov.hmrc.mongo.lock.{ LockRepository, LockService }

import java.time.LocalTime
import java.util.concurrent.{ ConcurrentLinkedQueue, TimeUnit }
import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.concurrent.{ ExecutionContext, Future }

class ScheduledStreamSpec
    extends TestKit(ActorSystem("ScheduledStreamSpec")) with ImplicitSender with AnyWordSpecLike with Matchers
    with BeforeAndAfterAll with ScalaFutures {

  implicit val ec: ExecutionContext = system.dispatcher
  val logger = Logger(getClass)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  // Helper to create config
  def testConfig(
    jobName: String,
    initialDelay: FiniteDuration = 100.milliseconds,
    interval: FiniteDuration = 100.milliseconds
  ): ScheduledJobConfig = {

    val delay = initialDelay
    val inter = interval

    val jobConfig = ConfigFactory.parseString(s"""microservice.$jobName.jobs {
                                                 |  user-counts = {
                                                 |    audit-type = "yta-mis-1"
                                                 |    service = my-service
                                                 |    api = "/admin/statistics"
                                                 |  },
                                                 |  something-else = {
                                                 |    audit-type = "yta-mis-2"
                                                 |    service = my-service
                                                 |    api = "/admin/statistics"
                                                 |  }
                                                 |}""".stripMargin)
    val configuration = Configuration(
      s"microservice.$jobName.schedule" -> "10:00" // Ignored - see delay overrides
    ).withFallback(Configuration(jobConfig))

    new ScheduledJobConfig(configuration, jobName) {
      override lazy val initialDelay: FiniteDuration = delay
      override lazy val interval: FiniteDuration = inter
    }
  }

  "Scheduled stream" should {

    "execute workload on each tick" in new Setup {
      var executionCount = 0

      val stream = ScheduledStream
        .builder(
          config = testConfig("test-stream"),
          name = "test-stream",
          sink = sink, // Pass the materialized sink
          logger = logger
        )
        .withWorkload {
          executionCount += 1
          Future.successful(())
        }
        .build()

      stream.start()

      // Now use the probe to test
      testProbe.request(1)
      testProbe.expectNext(500.millis, ())
      executionCount shouldBe 1

      testProbe.request(1)
      testProbe.expectNext(500.millis, ())
      executionCount shouldBe 2

      stream.stop()
      testProbe.cancel()
    }

    "skip execution when conditional returns false" in new Setup {
      var executionCount = 0
      var conditionalFlag = false

      val stream = ScheduledStream
        .builder(
          config = testConfig("conditional-stream", initialDelay = 50.milliseconds),
          name = "conditional-stream",
          sink = sink,
          logger = logger
        )
        .withWorkload {
          executionCount += 1
          Future.successful(())
        }
        .withConditional {
          conditionalFlag
        }
        .build()

      stream.start()

      // First tick - conditional is false, should complete but not execute workload
      testProbe.request(1)
      testProbe.expectNext(200.millis, ())
      executionCount shouldBe 0

      // Set conditional to true
      conditionalFlag = true

      // Second tick - should execute
      testProbe.request(1)
      testProbe.expectNext(200.millis, ())
      executionCount shouldBe 1

      stream.stop()
      testProbe.cancel()
    }

    "recover from workload failures" in new Setup {
      var executionCount = 0

      val stream = ScheduledStream
        .builder(
          config = testConfig("failing-stream", interval = 50.milliseconds),
          name = "failing-stream",
          sink = sink,
          logger = logger
        )
        .withWorkload {
          executionCount += 1
          if (executionCount == 1) {
            Future.failed(new RuntimeException("Test failure"))
          } else {
            Future.successful(())
          }
        }
        .build()

      stream.start()

      // First tick - fails but recovers
      testProbe.request(1)
      testProbe.expectNext()
      executionCount shouldBe 1

      // Second tick - succeeds
      testProbe.request(1)
      testProbe.expectNext()
      executionCount shouldBe 2

      stream.stop()
      testProbe.cancel()
    }

    import scala.jdk.CollectionConverters.*
    "respect configured intervals" in new Setup {
      val executionTimes = new ConcurrentLinkedQueue[Long]()
      val startTime = System.currentTimeMillis()

      val stream = ScheduledStream
        .builder(
          config = testConfig(
            "timed-stream",
            initialDelay = 100.millis,
            interval = 100.millis
          ),
          name = "timed-stream",
          sink = sink,
          logger = logger
        )
        .withWorkload {
          executionTimes.add(System.currentTimeMillis() - startTime)
          Future.successful(())
        }
        .build()

      stream.start()

      // Request and wait for 3 ticks
      testProbe.request(3)
      testProbe.expectNext(200.millis) // First tick at ~100ms
      testProbe.expectNext(200.millis) // Second tick at ~200ms
      testProbe.expectNext(200.millis) // Third tick at ~300ms

      val times = executionTimes.asScala.toList
      times.size shouldBe 3

      // Verify initial delay
      times.head should be(100L +- 50L)

      // Verify intervals between executions
      val intervals = times
        .sliding(2)
        .map {
          case List(a, b) => b - a
          case _          => 0L
        }
        .toList
      intervals.foreach { interval =>
        interval should be(100L +- 50L) // Should be ~100ms between executions
      }

      stream.stop()
    }
  }

  trait Setup {
    // Create the test sink
    val (testProbe, sink) = TestSink
      .probe[Unit](system)
      .preMaterialize() // This gives us both the probe and the sink
  }

}
