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
import play.api.Configuration
import repositories.UserFileRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadService @Inject() (
                                    fileRepository: UserFileRepository,
                                    upscanInitiateConnector: UpscanInitiateConnector,
                                    configuration: Configuration
                                  )(implicit ec: ExecutionContext) {

  private val minimumFileSize: Long   = configuration.underlying.getBytes("upscan.minFileSize")
  private val maximumFileSize: Long   = configuration.underlying.getBytes("upscan.maxFileSize")

  def initiate(userId: String)(implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] = {

    val request = UpscanInitiateRequest(
      callbackUrl = "callbackUrl", // TODO update this when the callback controller is added
      successRedirect = "successRedirect", // TODO update this when the upload controller is added
      errorRedirect = "errorRedirect", // TODO update this when the upload controller is added
      minimumFileSize = minimumFileSize,
      maximumFileSize = maximumFileSize
    )

    for {
      initiatedResponse <- upscanInitiateConnector.initiate(request)
      _                 <- fileRepository.initiate(userId, UploadedFile.Initiated(initiatedResponse.reference))
    } yield initiatedResponse
  }

  def update(uploadedFile: UploadedFile): Future[Done] =
    fileRepository.update(uploadedFile)
}
