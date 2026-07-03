/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.services

import play.api.libs.json.{ JsNull, JsValue }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{ LockService, MongoLockRepository }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.stats.connectors.ServiceConnector

import javax.inject.{ Inject, Singleton }
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class StatsGeneratorService @Inject() (
  serviceConnector: ServiceConnector,
  connector: AuditConnector,
  mongoLockRepository: MongoLockRepository
)(implicit ec: ExecutionContext)
    extends StatsService {

  val name: String = "stats-generator"

  override def run(service: String, api: String)(implicit hc: HeaderCarrier): Future[JsValue] =
    serviceConnector.generate(service, api).map(_ => JsNull)

  override def auditConnector: AuditConnector = connector

  override def lockKeeper: LockService =
    LockService(mongoLockRepository, lockId = "stats-generator-lock", ttl = 5.minutes)
}
