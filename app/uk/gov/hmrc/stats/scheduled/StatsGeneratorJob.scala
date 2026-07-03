/*
 * Copyright 2025 HM Revenue & Customs
 *
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
