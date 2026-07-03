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

package uk.gov.hmrc.stats.scheduled

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Sink
import play.api.{ Configuration, Logger }
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.stats.config.ScheduledJobConfig
import uk.gov.hmrc.stats.services.StatsGeneratorService

import javax.inject.{ Inject, Singleton }

@Singleton
class StatsGeneratorJob @Inject() (
  configuration: Configuration,
  service: StatsGeneratorService,
  lifecycle: ApplicationLifecycle,
  sink: Sink[Unit, ?] = Sink.ignore
)(implicit actorSystem: ActorSystem) {
  val name: String = service.name
  val config = ScheduledJobConfig(configuration, name)
  val logger: Logger = Logger(getClass)

  val s = ScheduledStreamCommon(name, config, service, lifecycle, logger, sink)
  s.stream.start()
}
