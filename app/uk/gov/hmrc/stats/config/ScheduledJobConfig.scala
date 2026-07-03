/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.config

import com.typesafe.config.Config
import play.api.Configuration
import net.ceedubs.ficus.Ficus.*
import uk.gov.hmrc.stats.model.StatsJob

import java.time.format.DateTimeFormatter
import java.time.{ Duration, LocalDateTime, LocalTime, ZoneId, ZonedDateTime }
import scala.concurrent.duration.{ FiniteDuration, HOURS, SECONDS }

class ScheduledJobConfig(config: Configuration, val name: String) {
  val zoneId: ZoneId = ZoneId.of("Europe/London")
  val configuration: Config = config.underlying

  private val TWENTY_FOUR: Long = 24

  lazy val schedule: String = configuration.as[String](s"microservice.$name.schedule")
  lazy val hour: Int = schedule.split(":")(0).toInt
  lazy val minute: Int = schedule.split(":")(1).toInt
  lazy val jobs: Set[StatsJob] = readJobs(configuration)
  lazy val initialDelay: FiniteDuration = FiniteDuration(secondsUntilNextExecution(), SECONDS)
  lazy val interval: FiniteDuration = FiniteDuration(TWENTY_FOUR, HOURS)

  def readJobs(configuration: Config): Set[StatsJob] =
    configuration
      .as[Map[String, Config]](s"microservice.$name.jobs")
      .map { case (job, values) =>
        StatsJob(
          name = job,
          auditType = values.as[String]("audit-type"),
          service = values.as[String]("service"),
          api = values.as[String]("api")
        )
      }
      .toSet

  def secondsUntilNextExecution(implicit now: ZonedDateTime = ZonedDateTime.now(zoneId)): Long = {
    val targetTime = LocalTime.parse(schedule, DateTimeFormatter.ofPattern("H:mm"))

    // Try today first
    val todayExecution = now.toLocalDate.atTime(targetTime).atZone(zoneId)

    val nextExecution = if (now.isBefore(todayExecution)) {
      todayExecution // Still time today
    } else {
      todayExecution.plusDays(1) // Tomorrow
    }
    Duration.between(now, nextExecution).getSeconds
  }
}
