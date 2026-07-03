/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.connectors

import javax.inject.{ Inject, Singleton }
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class ServiceConnector @Inject() (httpClient: HttpClientV2, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) {

  import uk.gov.hmrc.http.HttpReads.Implicits._

  private def makeUrl(service: String, path: String) = {
    val baseUrl = servicesConfig.baseUrl(service)
    new URL(s"$baseUrl$path")
  }

  def generate(service: String, path: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient.post(makeUrl(service, path)).withBody(Json.toJson("")).execute[HttpResponse]

  def collect(service: String, path: String)(implicit hc: HeaderCarrier): Future[JsValue] =
    httpClient.get(makeUrl(service, path)).execute[JsValue]

}
