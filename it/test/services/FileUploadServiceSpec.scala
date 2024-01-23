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

package services

import connectors.UpscanInitiateConnector
import models.UploadedFile
import models.upscan.{UpscanInitiateRequest, UpscanInitiateResponse}
import org.apache.pekko.Done
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import repositories.UserFileRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class FileUploadServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar with GuiceOneAppPerSuite {

  private val mockUpscanConnector: UpscanInitiateConnector = mock[UpscanInitiateConnector]
  private val mockUserFileRepository: UserFileRepository = mock[UserFileRepository]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "upscan.minFileSize" -> "1337b",
      "upscan.maxFileSize" -> "1k"
    )
    .overrides(
      bind[UpscanInitiateConnector].toInstance(mockUpscanConnector),
      bind[UserFileRepository].toInstance(mockUserFileRepository)
    )
    .build()

  private lazy val service: FileUploadService = app.injector.instanceOf[FileUploadService]

  private val hc: HeaderCarrier = HeaderCarrier()

  "initiate" - {

    val initiateResponse = UpscanInitiateResponse(
      reference = "reference",
      uploadRequest = UpscanInitiateResponse.UploadRequest(
        href = "foobar",
        fields = Map("foo" -> "bar")
      )
    )

    val initiatedFile = UploadedFile.Initiated("reference")

    "must initiate an upscan journey and insert it into the user file repository" in {

      val request = UpscanInitiateRequest(
        callbackUrl = "callbackUrl",
        successRedirect = "successRedirect",
        errorRedirect = "errorRedirect",
        minimumFileSize = 1337,
        maximumFileSize = 1024
      )

      when(mockUpscanConnector.initiate(any)(any))
        .thenReturn(Future.successful(initiateResponse))

      when(mockUserFileRepository.initiate(any, any))
        .thenReturn(Future.successful(Done))

      val result = service.initiate("userId")(hc).futureValue

      result mustEqual initiateResponse

      verify(mockUpscanConnector).initiate(eqTo(request))(eqTo(hc))
      verify(mockUserFileRepository).initiate(eqTo("userId"), eqTo(initiatedFile))
    }

    "must fail if the UpscanInitiateConnector fails" in {

      when(mockUpscanConnector.initiate(any)(any))
        .thenReturn(Future.failed(new RuntimeException()))

      service.initiate("userId")(hc).failed.futureValue
    }

    "must fail if the UserFileRepository fails" in {

      when(mockUpscanConnector.initiate(any)(any))
        .thenReturn(Future.successful(initiateResponse))

      when(mockUserFileRepository.initiate(any, any))
        .thenReturn(Future.failed(new RuntimeException()))

      service.initiate("userId")(hc).failed.futureValue
    }
  }

  "update" - {

    val file = UploadedFile.Initiated("foo")

    "must update the user file with the new one" in {

      when(mockUserFileRepository.update(any))
        .thenReturn(Future.successful(Done))

      service.update(file).futureValue

      verify(mockUserFileRepository).update(file)
    }

    "must fail when the user file repository fails" in {

      when(mockUserFileRepository.update(any))
        .thenReturn(Future.failed(new RuntimeException()))

      service.update(file).failed.futureValue
    }
  }
}
