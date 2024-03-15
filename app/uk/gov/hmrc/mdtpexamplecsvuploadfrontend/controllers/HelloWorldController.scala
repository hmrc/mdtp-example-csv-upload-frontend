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

import play.api.Configuration
import uk.gov.hmrc.mdtpexamplecsvuploadfrontend.views.html.HelloWorldPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class HelloWorldController @Inject()(
  mcc: MessagesControllerComponents,
  helloWorldPage: HelloWorldPage, configuration: Configuration)
    extends FrontendController(mcc) {

  val helloWorld: Action[AnyContent] = Action.async { implicit request =>
    val isFeatureEnabled = configuration.get[Boolean]("microservice.features.helloWorld")

    if (!isFeatureEnabled) {
      Future.successful(NotFound)
    } else {
      Future.successful(Ok(helloWorldPage()))
    }
  }
}
