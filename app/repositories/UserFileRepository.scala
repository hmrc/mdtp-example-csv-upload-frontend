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

import cats.implicits.toFunctorOps
import models.{UploadedFile, UserFile}
import org.apache.pekko.Done
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import play.api.Configuration
import repositories.UserFileRepository.NothingToUpdateException
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

@Singleton
class UserFileRepository @Inject()(
                                    mongoComponent: MongoComponent,
                                    clock: Clock,
                                    configuration: Configuration
                                  )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UserFile](
    mongoComponent = mongoComponent,
    collectionName = "uploaded-files",
    domainFormat = UserFile.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("userId", "uploadedFile.reference"),
        IndexOptions()
          .unique(true)
          .name("userIdReferenceIdx")
      ),
      IndexModel(
        Indexes.ascending("updatedAt"),
        IndexOptions()
          .expireAfter(configuration.get[Duration]("user-file-repository.ttl").toSeconds, TimeUnit.SECONDS)
          .name("updatedAtIdx")
      ),
      IndexModel(
        Indexes.ascending("uploadedFile.reference"),
        IndexOptions()
          .unique(true)
          .name("ReferenceIdx")
      )
    ),
    extraCodecs = Codecs.playFormatSumCodecs(UploadedFile.format)
  ) {

  def getFile(userId: String, reference: String): Future[Option[UploadedFile]] =
    collection.find(userAndReferenceFilter(userId, reference))
      .headOption().map(_.map(_.uploadedFile))

  def initiate(userId: String, uploadedFile: UploadedFile): Future[Done] =
    collection.replaceOne(
        userAndReferenceFilter(userId, uploadedFile.reference),
        UserFile(userId, uploadedFile, clock.instant()),
        ReplaceOptions().upsert(true)
      ).toFuture().as(Done)

  def update(uploadedFile: UploadedFile): Future[Done] =
    collection.findOneAndUpdate(
      Filters.eq("uploadedFile.reference", uploadedFile.reference),
      Updates.combine(
        Updates.set("uploadedFile", uploadedFile),
        Updates.set("updatedAt", clock.instant())
      )
    ).headOption().flatMap {
      _.as(Future.successful(Done))
        .getOrElse(Future.failed(NothingToUpdateException))
    }

  private def userAndReferenceFilter(userId: String, reference: String): Bson =
    Filters.and(
      Filters.eq("userId", userId),
      Filters.eq("uploadedFile.reference", reference)
    )
}

object UserFileRepository {

  case object NothingToUpdateException extends Exception with NoStackTrace {
    override def getMessage: String = "Unable to find uploaded file"
  }
}
