/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.libs.json.JsNull
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.stats.connectors.ServiceConnector

import scala.concurrent.{ ExecutionContext, Future }

class StatsGeneratorServiceSpec
    extends PlaySpec with ScalaFutures with MockitoSugar with BeforeAndAfterEach with IntegrationPatience {

  override def beforeEach(): Unit = {
    when(
      mockServiceConnector.generate(ArgumentMatchers.eq("stats-collector"), ArgumentMatchers.eq("test"))(
        any[HeaderCarrier]
      )
    )
      .thenReturn(Future.successful(HttpResponse(OK, "success")))
    ()
  }

  "StatsGeneratorService" should {
    "run" in {
      statsGeneratorServiceTest.run("stats-collector", "test").futureValue mustBe JsNull
    }
  }

  val mockServiceConnector: ServiceConnector = mock[ServiceConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockMongoLockRepository: MongoLockRepository = mock[MongoLockRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val statsGeneratorServiceTest =
    new StatsGeneratorService(mockServiceConnector, mockAuditConnector, mockMongoLockRepository)
}
