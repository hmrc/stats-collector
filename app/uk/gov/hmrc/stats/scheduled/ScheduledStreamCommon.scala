/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.scheduled

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Sink
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.stats.config.ScheduledJobConfig
import uk.gov.hmrc.stats.model.{ ProcessedJob, Result }
import uk.gov.hmrc.stats.services.StatsService

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class ScheduledStreamCommon(
  name: String,
  config: ScheduledJobConfig,
  service: StatsService,
  lifecycle: ApplicationLifecycle,
  logger: Logger,
  sink: Sink[Unit, ?] = Sink.ignore
)(implicit actorSystem: ActorSystem) {
  private implicit val ec: ExecutionContext = actorSystem.dispatcher

  val stream: ScheduledStream = ScheduledStream
    .builder(config, name, sink, logger, Some(lifecycle))(actorSystem)
    .withWorkload {
      Try {
        implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()
        service
          .lockAndProcessDueJobs(config.jobs)
          .map {
            case None                                                 => Result("Executed jobs: None")
            case maybeJob if maybeJob.isEmpty || maybeJob.get.isEmpty => Result("Executed jobs: Empty")
            case Some(executedJobs) =>
              val result = executedJobs.foldLeft("")((result, job) =>
                s"($result${job.jobName} - Success: ${job.status}${errorMsg(job)})"
              )
              Result("Executed jobs:" + result)
          }
          .recover { case ex =>
            Result(s"Error occurred : $ex")
          }
          .map { result =>
            logger.warn(result.message)
          }
      } match {
        case Success(value) =>
          value
        case Failure(exception) =>
          logger.error(exception.getMessage)
          Future.successful(())
      }
    }
    .build()

  private def errorMsg(jobResult: ProcessedJob) = if (jobResult.status) "" else s", Reason: ${jobResult.errorMessage}"

}
