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

package io.odin.loggers

import io.odin.{LoggerMessage, OdinSpec}

import cats.data.Writer
import cats.effect.Clock
import cats.Id

class WriterTLoggerSpec extends OdinSpec {

  type F[A] = Writer[List[LoggerMessage], A]

  implicit val clock: Clock[Id] = zeroClock

  checkAll(
    "WriterTLogger",
    LoggerTests[F](new WriterTLogger[Id], _.written).all
  )

  it should "write log into list" in {
    val logger = new WriterTLogger[Id]()
    forAll { (msg: LoggerMessage) =>
      logger.log(msg).written shouldBe List(msg)
    }
  }

  it should "write all the logs into list" in {
    val logger = new WriterTLogger[Id]()
    forAll { (msgs: List[LoggerMessage]) =>
      logger.log(msgs).written shouldBe msgs
    }
  }

}
