/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.stats.connectors

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ verify, when }
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.http.Status.OK
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class ServiceConnectorSpec extends PlaySpec with ScalaFutures {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "Service connector collect" should {
    "return stats" in new TestBase {
      when(httpClientV2.get(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[JsValue](any, any)).thenReturn(Future.successful(exampleStats))

      private val result = connector.collect("test-service", "/stats/users-count")
      verify(httpClientV2).get(new URL("http://somehost/test-service/stats/users-count"))
      verify(requestBuilder).execute[JsValue]
      result.futureValue must be(exampleStats)
    }
  }

  "Service connector generate" should {
    "return ok" in new TestBase {
      when(httpClientV2.post(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any)(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(OK, "")))

      private val response = connector.generate("test-service", "/stats/users-count").futureValue
      verify(httpClientV2).post(new URL("http://somehost/test-service/stats/users-count"))
      response.status must be(OK)
    }
  }
}

trait TestBase extends MockitoSugar {

  val exampleStats: JsValue = Json.parse("""{
    "totalOfAllPreferences":{
      "count":14,
      "date":"2016-06-22"
    }}""")

  val httpClientV2: HttpClientV2 = mock[HttpClientV2]
  val requestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  when(mockServicesConfig.baseUrl(any[String])).thenReturn("http://somehost/test-service")

  val connector: ServiceConnector = new ServiceConnector(httpClientV2, mockServicesConfig)

}
