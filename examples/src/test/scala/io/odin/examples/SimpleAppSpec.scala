/*
 * Copyright 2024 ScalaFreaks
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

package io.odin.examples

import io.odin.{Logger, LoggerMessage, OdinSpec}
import io.odin.loggers.WriterTLogger

import cats.data.WriterT
import cats.effect.unsafe.IORuntime
import cats.effect.IO

/**
  * This spec shows an example of how to test the logger using WriterT monad that is just an abstraction over `F[(Log, A)]`
  */
class SimpleAppSpec extends OdinSpec {

  // definition of test monad. It keeps all the incoming `LoggerMessage` inside of the list
  type WT[A] = WriterT[IO, List[LoggerMessage], A]
  implicit val ioRuntime: IORuntime = IORuntime.global

  "HelloSimpleService" should "log greeting call" in {
    // logger that writes messages as a log of WriterT monad
    val logger: Logger[WT]                    = new WriterTLogger[IO]
    val simpleService: HelloSimpleService[WT] = new HelloSimpleService(logger)

    val name = "UserName"

    // .written is the method of WriterT monad that returns IO[List[LoggerMessage]]
    val loggedMessage :: Nil = simpleService.greet(name).written.unsafeRunSync(): @unchecked

    // LoggerMessage.message contains the lazy evaluated log from simpleService.greet method
    loggedMessage.message.value shouldBe s"greet is called by user $name"
  }

}
