/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.services

import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.{ LockService, MongoLockRepository }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.stats.connectors.ServiceConnector

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

@Singleton
class StatsCollectorService @Inject() (
  serviceConnector: ServiceConnector,
  connector: AuditConnector,
  mongoLockRepository: MongoLockRepository
) extends StatsService {

  override val name: String = "stats-collector"

  override def run(service: String, api: String)(implicit hc: HeaderCarrier): Future[JsValue] =
    serviceConnector.collect(service, api)

  override def auditConnector: AuditConnector = connector

  override def lockKeeper: LockService =
    LockService(mongoLockRepository, lockId = "stats-collector-lock", ttl = 5.minutes)
}
