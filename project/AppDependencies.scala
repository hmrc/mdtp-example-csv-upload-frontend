import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val hmrcMongoVersion = "1.7.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30" % "8.3.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "com.beachape"            %% "enumeratum-play"            % "1.8.0",
    "org.typelevel"           %% "cats-core"                  % "2.10.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion            % Test,
    "org.jsoup"               %  "jsoup"                      % "1.13.1"            % Test,
  )

  val it = Seq.empty
}
