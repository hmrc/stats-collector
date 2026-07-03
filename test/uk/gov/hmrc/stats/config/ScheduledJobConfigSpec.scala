/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.config

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.stats.model.StatsJob

import java.time.format.DateTimeFormatter
import java.time.{ Duration, Instant, LocalTime, ZonedDateTime }

class ScheduledJobConfigSpec extends PlaySpec {
  val name: String = "stats"

  "process empty config and return an empty collection" in {

    val config = ConfigFactory.parseString(s"""microservice.$name.jobs {
                                              |}""".stripMargin)

    val scheduledJobConfig = new ScheduledJobConfig(Configuration(config), name)
    scheduledJobConfig.jobs must be(Set())
  }

  "read valid values from the config into a stats job collection" in {
    val config = ConfigFactory.parseString(s"""microservice.$name.jobs {
                                              |  user-counts = {
                                              |    audit-type = "yta-mis-1"
                                              |    service = my-service
                                              |    api = "/admin/statistics"
                                              |  },
                                              |  something-else = {
                                              |    audit-type = "yta-mis-2"
                                              |    service = my-service
                                              |    api = "/admin/statistics"
                                              |  }
                                              |}""".stripMargin)

    val scheduledJobConfig = new ScheduledJobConfig(Configuration(config), name)
    scheduledJobConfig.jobs must be(
      Set(
        StatsJob("user-counts", "yta-mis-1", "my-service", "/admin/statistics"),
        StatsJob("something-else", "yta-mis-2", "my-service", "/admin/statistics")
      )
    )
  }

  "calculate the right initial delay" in {
    val config = ConfigFactory.parseString(s"""microservice.$name.schedule = "11:00"""".stripMargin)
    val scheduledJobConfig = new ScheduledJobConfig(Configuration(config), name)

    scheduledJobConfig.schedule mustBe "11:00"
    scheduledJobConfig.hour mustBe 11
    scheduledJobConfig.minute mustBe 0

    implicit val nowBefore: ZonedDateTime = ZonedDateTime
      .now(scheduledJobConfig.zoneId)
      .withHour(10)
      .withMinute(45)
      .withSecond(0)
      .withNano(0)

    val calc = scheduledJobConfig.secondsUntilNextExecution(nowBefore)
    calc mustBe timeToSeconds(0, 15)
  }

  "calculate the right initial delay, after schedule" in {
    val config = ConfigFactory.parseString(s"""microservice.$name.schedule = "11:00"""".stripMargin)
    val scheduledJobConfig = new ScheduledJobConfig(Configuration(config), name)

    scheduledJobConfig.schedule mustBe "11:00"
    scheduledJobConfig.hour mustBe 11
    scheduledJobConfig.minute mustBe 0

    implicit val nowAfter: ZonedDateTime = ZonedDateTime
      .now(scheduledJobConfig.zoneId)
      .withHour(11)
      .withMinute(15)
      .withSecond(0)
      .withNano(0)

    val calc = scheduledJobConfig.secondsUntilNextExecution(nowAfter)
    calc mustBe timeToSeconds(23, 45)
  }

  "calculate the right initial delay close to midnight" in {
    val config = ConfigFactory.parseString(s"""microservice.$name.schedule = "23:00"""".stripMargin)
    val scheduledJobConfig = new ScheduledJobConfig(Configuration(config), name)

    scheduledJobConfig.schedule mustBe "23:00"
    scheduledJobConfig.hour mustBe 23
    scheduledJobConfig.minute mustBe 0

    implicit val nowAfter: ZonedDateTime = ZonedDateTime
      .now(scheduledJobConfig.zoneId)
      .withHour(23)
      .withMinute(30)
      .withSecond(0)
      .withNano(0)

    val calc = scheduledJobConfig.secondsUntilNextExecution(nowAfter)
    calc mustBe timeToSeconds(23, 30)
  }

  "calculate the right initial delay schedule after midnight" in {
    val config = ConfigFactory.parseString(s"""microservice.$name.schedule = "01:00"""".stripMargin)
    val scheduledJobConfig = new ScheduledJobConfig(Configuration(config), name)

    scheduledJobConfig.schedule mustBe "01:00"
    scheduledJobConfig.hour mustBe 1
    scheduledJobConfig.minute mustBe 0

    implicit val nowAfter: ZonedDateTime = ZonedDateTime
      .now(scheduledJobConfig.zoneId)
      .withHour(23)
      .withMinute(30)
      .withSecond(0)
      .withNano(0)

    val calc = scheduledJobConfig.secondsUntilNextExecution(nowAfter)
    calc mustBe timeToSeconds(1, 30)
  }

  private def timeToSeconds(hours: Int, minutes: Int): Long =
    Duration.ofHours(hours).plusMinutes(minutes).getSeconds
}
