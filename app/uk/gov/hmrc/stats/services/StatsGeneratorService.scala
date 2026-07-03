/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
