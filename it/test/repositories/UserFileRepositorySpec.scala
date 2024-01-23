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

package repositories

import models.{UploadedFile, UserFile}
import org.mongodb.scala.model.{Filters, Updates}
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}

class UserFileRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[UserFile] {

  private val clock = Clock.fixed(Instant.now.truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC)

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[Clock].toInstance(clock)
    )
    .configure(
      "user-file-repository.ttl" -> "15 minutes"
    )
    .build()

  override protected lazy val repository: UserFileRepository =
    app.injector.instanceOf[UserFileRepository]

  private val initiatedFile: UploadedFile = UploadedFile.Initiated("foo")

  private val initiatedFile2: UploadedFile = UploadedFile.Initiated("bar")

  private val successfulFile: UploadedFile = UploadedFile.Success(
    reference = "foo",
    downloadUrl = "downloadUrl",
    uploadDetails = UploadedFile.UploadDetails(
      fileName = "somefile.txt",
      fileMimeType = "text/plain",
      uploadTimestamp = clock.instant(),
      checksum = "checksum",
      size = 1337
    )
  )

  "getFile" - {

    "must return `None` when no file exists with that id for that user" in {

      repository.initiate("user1", initiatedFile).futureValue
      repository.initiate("user2", initiatedFile2).futureValue
      repository.getFile("user1", "bar").futureValue mustBe empty
    }

    "must return the uploaded file when it exists" in {

      repository.initiate("user1", initiatedFile).futureValue
      repository.initiate("user1", initiatedFile2).futureValue
      repository.getFile("user1", "foo").futureValue.value mustEqual initiatedFile
    }
  }

  "initiate" - {

    "must insert the user file when there isn't one already" in {

      repository.getFile("user1", "foo").futureValue mustBe empty
      repository.initiate("user1", initiatedFile).futureValue
      repository.getFile("user1", "foo").futureValue.value mustEqual initiatedFile

      val userFile = repository.collection.find(
        Filters.and(
          Filters.eq("userId", "user1"),
          Filters.eq("uploadedFile.reference", "foo")
        )
      ).headOption().futureValue.value

      userFile.updatedAt mustEqual clock.instant()
    }

    "must update the user file when there is one already" in {

      repository.initiate("user1", initiatedFile).futureValue
      repository.initiate("user1", successfulFile).futureValue
      repository.getFile("user1", "foo").futureValue.value mustEqual successfulFile
    }

    "must fail if there is a file with the same reference for a different user" in {
      repository.initiate("user1", initiatedFile).futureValue
      repository.initiate("user2", initiatedFile).failed.futureValue
    }
  }

  "update" - {

    "must update the user file" in {

      repository.initiate("user1", initiatedFile).futureValue
      repository.collection.updateOne(
        Filters.eq("uploadedFile.reference", "foo"),
        Updates.set("updatedAt", clock.instant().minus(10, ChronoUnit.DAYS))
      ).toFuture().futureValue

      repository.update(successfulFile).futureValue
      repository.getFile("user1", "foo").futureValue.value mustEqual successfulFile

      val uploadedAt = repository.collection
        .find(Filters.eq("uploadedFile.reference", "foo"))
        .headOption().futureValue.value.updatedAt

      uploadedAt mustEqual clock.instant()
    }

    "must fail if there is no existing file" in {

      repository.update(successfulFile).failed.futureValue mustEqual UserFileRepository.NothingToUpdateException
    }
  }
}
