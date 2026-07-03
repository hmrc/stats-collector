/*
 * Copyright 2023 HM Revenue & Customs
 *
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
