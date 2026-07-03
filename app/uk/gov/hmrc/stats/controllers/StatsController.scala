/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.controllers

import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, ControllerComponents }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.stats.model.ProcessedJob
import uk.gov.hmrc.stats.scheduled.{ StatsCollectorJob, StatsGeneratorJob }
import uk.gov.hmrc.stats.services.*

import scala.concurrent.ExecutionContext

@Singleton
class StatsController @Inject() (
  val cc: ControllerComponents,
  collectorService: StatsCollectorService,
  generatorService: StatsGeneratorService,
  statsGeneratorJob: StatsGeneratorJob,
  statsCollectorJob: StatsCollectorJob
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  private val toJson = (stats: Set[ProcessedJob]) =>
    Ok(Json.toJson(stats.foldLeft(Map.empty[String, ProcessedJob])((map, job) => map + (job.jobName -> job))))

  def generate: Action[AnyContent] = Action.async { implicit request =>
    generatorService.processDueJobs(statsGeneratorJob.config.jobs).map(toJson)
  }

  def collect: Action[AnyContent] = Action.async { implicit request =>
    collectorService.processDueJobs(statsCollectorJob.config.jobs).map(toJson)
  }
}
