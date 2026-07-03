/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ verify, verifyNoMoreInteractions, when }
import org.mockito.{ ArgumentCaptor, ArgumentMatchers, Mockito }
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.{ BeforeAndAfterEach, Inside, LoneElement }
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{ Lock, LockRepository, LockService }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.stats.model.StatsJob

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{ Duration, DurationInt }
import scala.concurrent.{ ExecutionContext, Future }

class StatsServiceSpec
    extends PlaySpec with ScalaFutures with LoneElement with Inside with MockitoSugar with BeforeAndAfterEach
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockRepository: LockRepository = mock[LockRepository]

  override protected def beforeEach(): Unit = {
    when(mockRepository.takeLock(ArgumentMatchers.eq("test-stats"), any[String](), ArgumentMatchers.eq(10.seconds)))
      .thenReturn(Future.successful(Some(Lock("id", "owner", Instant.now(), Instant.now()))))
    when(mockRepository.releaseLock(ArgumentMatchers.eq("test-stats"), any[String]()))
      .thenReturn(Future.successful(()))
    ()
  }
  "stats collector" should {

    "process single job" in new TestStatsService {

      override val result: Future[JsValue] = Future.successful(Json.obj("some" -> "json"))
      private val job = StatsJob("user-counts", "yta-mis", "my-service", "/admin/statistics")

      private val pr = lockAndProcessDueJobs(Set(job)).futureValue.get

      pr.loneElement must have(
        Symbol("jobName")("user-counts"),
        Symbol("status")(true)
      )

      val e: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(auditConnector).sendExtendedEvent(e.capture)(any[HeaderCarrier], any[ExecutionContext])

      e.getValue must have(
        Symbol("auditSource")("test-stats"),
        Symbol("auditType")("yta-mis"),
        Symbol("tags")(
          Map(
            "transactionName" -> job.name
          )
        ),
        Symbol("detail")(result.value.get.get)
      )
    }

    "get error message in case of an exception from the job execution" in new TestStatsService {
      private val job = StatsJob("user-counts", "yta-mis", "my-service", "/admin/statistics")

      override val result: Future[JsValue] = Future.failed(new RuntimeException("some error from the service"))

      private val processedJobs = lockAndProcessDueJobs(Set(job))

      processedJobs.futureValue.get.loneElement must have(
        Symbol("jobName")(job.name),
        Symbol("status")(false),
        Symbol("errorMessage")("some error from the service")
      )

      verifyNoMoreInteractions(auditConnector)
    }

    "process multiple jobs" in new TestStatsService {
      override val result: Future[JsValue] = Future.successful(Json.obj("some" -> "value"))

      private val firstJob = StatsJob("test-one", "yta-one", "one", "/admin/one")
      private val secondJob = StatsJob("test-two", "yta-two", "two", "/admin/two")

      private val processedJobs = lockAndProcessDueJobs(Set(firstJob, secondJob))

      processedJobs.futureValue.get.size must be(2)

      inside(processedJobs.futureValue.get.toList) { case List(processedJobOne, processedJobTwo) =>
        processedJobOne must have(
          Symbol("jobName")(firstJob.name),
          Symbol("status")(true)
        )
        processedJobTwo must have(
          Symbol("jobName")(secondJob.name),
          Symbol("status")(true)
        )
      }

      verify(auditConnector, Mockito.times(2))
        .sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext])
    }

    "not release the lock" in new TestStatsService {
      override val result: Future[JsValue] = Future.successful(Json.obj("sss" -> "eee"))

      private val job = StatsJob("user-counts", "yta-mis", "my-service", "/admin/statistics")

      lockAndProcessDueJobs(Set(job)).futureValue

      verify(mockRepository, Mockito.times(4)).takeLock(any[String], any[String], any[Duration])
    }
  }

  trait TestStatsService extends StatsService {
    val auditConnector: AuditConnector = mock[AuditConnector]

    def result: Future[JsValue] = Future.successful(Json.obj("some" -> "json"))
    override val lockKeeper: LockService = new LockService {
      override val lockRepository: LockRepository = mockRepository
      override val lockId: String = "test-stats"
      override val ttl: Duration = 10.seconds
    }
    override def name: String = "test-stats"

    override def run(service: String, api: String)(implicit hc: HeaderCarrier): Future[JsValue] = result
  }
}
