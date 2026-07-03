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
