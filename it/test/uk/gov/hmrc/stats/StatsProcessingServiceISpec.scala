/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{ Application, Environment, Logger, Mode }
import play.api.libs.json.*
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSBodyWritables.writeableOf_String

import uk.gov.hmrc.stats.UrlHelper.-/

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StatsProcessingServiceISpec
    extends PlaySpec with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with BeforeAndAfterEach {

  private val logger = Logger(getClass)

  override def fakeApplication(): Application = {
    logger.info(s"""Starting application with additional config:
                   |  ${configMap.mkString("\n  ")}
                   |  """.stripMargin)
    // If applicationMode is not set, use Mode.Test (the default for GuiceApplicationBuilder)
    GuiceApplicationBuilder(environment = Environment.simple(mode = Some(Mode.Dev).getOrElse(Mode.Test)))
      .configure(configMap)
      .build()
  }

  def additionalConfig: Map[String, _] = Map(
    "microservice.stats-collector.jobs.test-job-one.audit-type"   -> "yta-mis-test-one",
    "microservice.stats-collector.jobs.test-job-one.audit-type"   -> "yta-mis-test-one",
    "microservice.stats-collector.jobs.test-job-one.service"      -> "preferences",
    "microservice.stats-collector.jobs.test-job-one.api"          -> "/preferences/stats",
    "microservice.stats-collector.jobs.test-job-two.audit-type"   -> "yta-mis-test-two",
    "microservice.stats-collector.jobs.test-job-two.service"      -> "preferences",
    "microservice.stats-collector.jobs.test-job-two.api"          -> "/preferences/stats",
    "microservice.stats-collector.jobs.test-job-three.audit-type" -> "yta-mis-test-three",
    "microservice.stats-collector.jobs.test-job-three.service"    -> "preferences",
    "microservice.stats-collector.jobs.test-job-three.api"        -> "/preferences/stats",
    "microservice.stats-collector.scheduler"                      -> "00:00",
    "auditing.enabled"                                            -> false
  )

  "The stats collector service" should {
    "process the job now and return the audit information, if no lastRun record found for the job" in {
      val wsClient = app.injector.instanceOf[WSClient]
      wsClient.url(resource("/admin/stats")).post("").futureValue
      val response = wsClient.url(resource("/admin/stats")).get().futureValue
      response must have(Symbol("status")(OK))
      (response.json \ "test-job-one" \ "jobName").as[String] must be("test-job-one")
      (response.json \ "test-job-one" \ "event" \ "auditSource").as[String] must be("stats-collector")
      (response.json \ "test-job-one" \ "event" \ "auditType").as[String] must be("yta-mis-test-one")
      (response.json \ "test-job-one" \ "event" \ "tags" \ "transactionName").as[String] must be("test-job-one")
      (response.json \ "test-job-one" \ "event" \ "detail") must not be a[JsUndefined]
    }
  }

  // This is not called mongoUri to avoid conflicts with mongo testing traits.
  protected def serviceMongoUri =
    s"mongodb://localhost:27017/${testId.toString}"

  private lazy val mongoConfig =
    Map(s"mongodb.uri" -> serviceMongoUri)

  private lazy val configMap =
    mongoConfig ++ additionalConfig

  def testName: String =
    getClass.getSimpleName

  protected val testId =
    TestId(testName)

  def resource(path: String): String =
    s"http://localhost:$port/${-/(path)}"
}

object UrlHelper {
  def -/(uri: String) =
    if (uri.startsWith("/")) uri.drop(1) else uri
}

case class TestId(testName: String) {

  val runId =
    DateTimeFormatter.ofPattern("HHmmssSSS").format(LocalDateTime.now())

  override val toString =
    s"${testName.toLowerCase.take(30)}-$runId"
}
