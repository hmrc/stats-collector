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

import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.stats.model.{ ProcessedJob, StatsJob }

import scala.concurrent.{ ExecutionContext, Future }

trait StatsService {
  def auditConnector: AuditConnector
  def lockKeeper: LockService
  def name: String
  def run(service: String, api: String)(implicit hc: HeaderCarrier): Future[JsValue]

  def lockAndProcessDueJobs(
    statsJobs: Set[StatsJob]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Set[ProcessedJob]]] =
    lockKeeper.withLock {
      processDueJobs(statsJobs)
    }

  def processDueJobs(
    statsJobs: Set[StatsJob]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[ProcessedJob]] =
    Future.traverse(statsJobs) { job =>
      processJob(job)
        .map { event =>
          ProcessedJob(job.name, status = true, event)
        }
        .recover { case exception => ProcessedJob(job.name, status = false, None, exception.getMessage) }
    }

  private def processJob(
    job: StatsJob
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ExtendedDataEvent]] =
    run(job.service, job.api).map { result =>
      val currentEvent = ExtendedDataEvent(
        auditSource = name,
        auditType = job.auditType,
        tags = Map("transactionName" -> job.name),
        detail = result
      )
      auditConnector.sendExtendedEvent(currentEvent)
      Some(currentEvent)
    }
}
