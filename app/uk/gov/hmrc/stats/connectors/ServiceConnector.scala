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
