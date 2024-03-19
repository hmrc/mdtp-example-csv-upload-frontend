/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.mdtpexamplecsvuploadfrontend.controllers

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._

class HelloWorldControllerSpec extends AnyWordSpec with Matchers with OptionValues {
  def app(featureEnabled: Boolean): Application =
    new GuiceApplicationBuilder()
      .configure("features.helloWorld" -> featureEnabled)
      .build()

  private def fakeRequest = FakeRequest(routes.HelloWorldController.helloWorld)

  "GET /" should {
    "return 200" in {
      val application = app(true)
      running(application) {

        val result = route(application, fakeRequest).value
        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }
    }

    "return 404 when feature flag disabled" in {
      val application = app(false)
      running(application) {
        val result = route(application, fakeRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }
    }
  }
}
