/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.utils

import scala.concurrent.duration.FiniteDuration

object FiniteDurationExtensions {
  implicit class FiniteDurationOps(duration: FiniteDuration) {
    def toPrettyString: String = {
      val totalSeconds = duration.toSeconds
      val hours = totalSeconds / 3600
      val minutes = (totalSeconds % 3600) / 60
      val seconds = totalSeconds  % 60

      (hours, minutes, seconds) match {
        case (0, 0, s) => s"${s}s"
        case (0, m, s) => s"${m}m ${s}s"
        case (h, m, s) => s"${h}h ${m}m ${s}s"
      }
    }
  }
}
