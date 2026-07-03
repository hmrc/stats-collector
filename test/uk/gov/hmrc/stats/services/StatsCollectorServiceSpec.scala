/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.stats.connectors.ServiceConnector

import scala.concurrent.Future

class StatsCollectorServiceSpec
    extends PlaySpec with ScalaFutures with MockitoSugar with BeforeAndAfterEach with IntegrationPatience {

  override def beforeEach(): Unit = {
    when(
      mockServiceConnector.collect(ArgumentMatchers.eq("stats-collector"), ArgumentMatchers.eq("test"))(
        any[HeaderCarrier]
      )
    )
      .thenReturn(Future.successful(Json.toJson("{}")))
    ()
  }

  "StatsCollectorService" should {
    "run" in {
      statsCollectorServiceTest.run("stats-collector", "test").futureValue mustBe Json.toJson("{}")
    }
  }

  val mockServiceConnector: ServiceConnector = mock[ServiceConnector]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockMongoLockRepository: MongoLockRepository = mock[MongoLockRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val statsCollectorServiceTest =
    new StatsCollectorService(mockServiceConnector, mockAuditConnector, mockMongoLockRepository)
}
