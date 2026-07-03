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
import org.apache.pekko.stream.scaladsl.{ Keep, Sink, Source }
import org.apache.pekko.stream.{ KillSwitch, KillSwitches }
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.stats.config.ScheduledJobConfig
import uk.gov.hmrc.stats.utils.FiniteDurationExtensions.*

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }

class ScheduledStream(
  config: ScheduledJobConfig,
  name: String,
  workload: () => Future[Unit],
  sink: Sink[Unit, ?] = Sink.ignore,
  logger: Logger,
  lifecycle: Option[ApplicationLifecycle] = None
)(implicit actorSystem: ActorSystem) {
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  private var killSwitch: Option[KillSwitch] = None

  def start(): Unit = {
    config.schedule
    logger.warn(s"$name stream is scheduled to start processing at ${config.schedule}, calculating initial delay")
    logger.warn(
      s"$name stream starting: initialDelay: ${config.initialDelay.toPrettyString}, " +
        s"interval: ${config.interval.toPrettyString}"
    )

    val (ks, _) = Source
      .tick(config.initialDelay, config.interval, ())
      .mapAsync(1) { _ =>
        workload().recover { case ex: Throwable =>
          logger.warn(s"Workload failed: ${ex.getMessage}")
          () // Continue with next tick
        }
      }
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(sink)(Keep.both)
      .run()

    killSwitch = Some(ks)

    lifecycle.foreach { lc =>
      lc.addStopHook { () =>
        logger.warn(s"$name shutting down stream...")
        stop()
        Future.successful(())
      }
    }
  }

  def stop(): Unit =
    killSwitch.foreach(_.shutdown())
}

class ScheduledStreamBuilder(
  config: ScheduledJobConfig,
  name: String,
  sink: Sink[Unit, ?] = Sink.ignore,
  logger: Logger,
  lifecycle: Option[ApplicationLifecycle] = None
)(implicit actorSystem: ActorSystem) {

  private var workload: () => Future[Unit] = () => Future.unit
  private var conditional: Option[() => Boolean] = None

  def withWorkload(w: => Future[Unit]): this.type = {
    workload = () => w
    this
  }

  def withConditional(c: => Boolean): this.type = {
    conditional = Some(() => c)
    this
  }

  def build(): ScheduledStream = {
    // Compose the final workload function
    val finalWorkload = composeWorkload(workload)
    new ScheduledStream(config, name, finalWorkload, sink, logger, lifecycle)(actorSystem)
  }

  private def composeWorkload(baseWorkload: () => Future[Unit]): () => Future[Unit] = { () =>
    // Apply conditional check
    conditional match {
      case Some(cond) if !cond() => Future.unit
      case _                     => baseWorkload()
    }
  }
}

object ScheduledStream {
  def builder(
    config: ScheduledJobConfig,
    name: String,
    sink: Sink[Unit, ?] = Sink.ignore,
    logger: Logger,
    lifecycle: Option[ApplicationLifecycle] = None
  )(implicit actorSystem: ActorSystem): ScheduledStreamBuilder =
    new ScheduledStreamBuilder(config, name, sink, logger, lifecycle)
}
