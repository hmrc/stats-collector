/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

import play.core.PlayVersion
import sbt._

object AppDependencies {

  val hmrcMongo = "2.12.0"
  val bootstrapVersion = "10.7.0"
  val pekkoVersion = "1.0.3"

  val compile = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % hmrcMongo,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "net.codingwell"    %% "scala-guice"               % "6.0.0",
    "com.iheart"        %% "ficus"                     % "1.5.2"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"    % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"   % hmrcMongo        % Test,
    "org.scalatestplus"      %% "mockito-3-4"               % "3.2.10.0"       % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"        % "7.0.2"          % Test,
    "org.apache.pekko"       %% "pekko-testkit"             % pekkoVersion     % Test,
    "org.apache.pekko"       %% "pekko-stream-testkit"      % pekkoVersion     % Test,
    "org.apache.pekko"       %% "pekko-actor-testkit-typed" % pekkoVersion     % Test
  )
}
