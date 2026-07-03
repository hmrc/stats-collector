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
