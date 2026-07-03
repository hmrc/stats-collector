/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.model

import play.api.libs.json.{ Json, OFormat, OWrites }
import uk.gov.hmrc.audit.serialiser.AuditSerialiser
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

case class StatsJob(name: String, auditType: String, service: String, api: String)

object StatsJob {
  implicit val statsJobFormats: OFormat[StatsJob] = Json.format[StatsJob]
}

case class ProcessedJob(jobName: String, status: Boolean, event: Option[ExtendedDataEvent], errorMessage: String = "")

object ProcessedJob {
  implicit val extendedDataEventWrites: OWrites[ExtendedDataEvent] = (o: ExtendedDataEvent) =>
    AuditSerialiser.serialise(o)
  implicit val processedJobWrites: OWrites[ProcessedJob] = Json.writes[ProcessedJob]
}
