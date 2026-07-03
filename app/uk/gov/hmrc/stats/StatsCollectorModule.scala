/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats

import com.google.inject.{ AbstractModule, Provides }
import net.codingwell.scalaguice.ScalaModule
import org.apache.pekko.stream.scaladsl.Sink
import uk.gov.hmrc.stats.scheduled.{ StatsCollectorJob, StatsGeneratorJob }

class StatsCollectorModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[StatsCollectorJob].asEagerSingleton()
    bind[StatsGeneratorJob].asEagerSingleton()
  }

  @Provides
  def sink(): Sink[Unit, ?] = Sink.ignore
}
