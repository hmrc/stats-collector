/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.scheduled

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.apache.pekko.testkit.TestKit
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.{ verify, when }
import org.mockito.internal.verification.VerificationModeFactory.times
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.{ Configuration, Logger }
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.stats.config.ScheduledJobConfig
import uk.gov.hmrc.stats.model.{ ProcessedJob, StatsJob }
import uk.gov.hmrc.stats.services.StatsService

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

class ScheduledStreamCommonSpec extends PlaySpec with BeforeAndAfterAll {
  val testKit = ActorTestKit()
  implicit val system: ActorSystem = testKit.system.classicSystem
  implicit val ec: ExecutionContext = system.dispatcher
  implicit lazy val materializer: Materializer = Materializer(system)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "Stats collector jobs" should {

    "emits elements correctly" in new Setup {
      when(service.lockAndProcessDueJobs(any)(any, any))
        .thenReturn(Future.successful(Some(Set(ProcessedJob("users-count", status = true, None)))))

      job.stream.start()
      probeSubscriber
        .request(1)
        .expectNext(())

      verify(service, times(1)).lockAndProcessDueJobs(any)(any, any)
    }

    "respect configured delays and intervals" in new Setup {
      when(service.lockAndProcessDueJobs(any)(any, any))
        .thenReturn(Future.successful(Some(Set(ProcessedJob("users-count", status = true, None)))))
      job.stream.start()
      val startTime = System.currentTimeMillis()

      probeSubscriber
        .request(2)
        .expectNext(()) // Should arrive after ~100ms

      val firstElementTime = System.currentTimeMillis()
      (firstElementTime - startTime) must be >= 100L

      probeSubscriber
        .expectNext(()) // Should arrive after another ~200ms

      val secondElementTime = System.currentTimeMillis()
      (secondElementTime - firstElementTime) must be >= 180L // Give it some leeway
      verify(service, times(2)).lockAndProcessDueJobs(any)(any, any)
    }

    "recover after an error" in new Setup {
      when(service.lockAndProcessDueJobs(any)(any, any))
        .thenThrow(new RuntimeException("oops"))
        .thenReturn(Future.successful(Some(Set(ProcessedJob("users-count", status = true, None)))))

      job.stream.start()

      probeSubscriber
        .request(2)
        .expectNext(())
        .expectNext(())

      verify(service, times(2)).lockAndProcessDueJobs(any)(any, any)
    }
  }

  trait Setup {
    val env: String = "Prod"
    val name = "stats"
    val service: StatsService = mock[StatsService]
    val config = ConfigFactory.parseString(s"""microservice.$name {
                                              |  schedule = "02:30",
                                              |  jobs = {
                                              |    print-suppression = {
                                              |      audit-type = "yta-mis"
                                              |      service = preferences
                                              |      api = "/preferences/stats"
                                              |    }
                                              |  }
                                              |}""".stripMargin)

    val configuration = Configuration(config)

    var scheduledJobConfig = new ScheduledJobConfig(configuration, name) {
      override lazy val initialDelay: FiniteDuration = FiniteDuration(100, TimeUnit.MILLISECONDS)
      override lazy val interval: FiniteDuration = FiniteDuration(200, TimeUnit.MILLISECONDS)
    }
    val lifecycle = mock[ApplicationLifecycle]
    val logger = Logger(getClass)
    val (probeSubscriber, probeSink) = TestSink.probe[Unit].preMaterialize()

    var job = ScheduledStreamCommon(name, scheduledJobConfig, service, lifecycle, logger, probeSink)
  }
}
