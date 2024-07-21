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

import java.util.concurrent.Executors

import cats.data.WriterT
import cats.effect.unsafe.IORuntime
import cats.effect.{Clock, IO}
import io.odin.syntax._
import io.odin.{LoggerMessage, OdinSpec}

import scala.concurrent.ExecutionContext

class ConstContextLoggerSpec extends OdinSpec {
  type F[A] = WriterT[IO, List[LoggerMessage], A]

  implicit val clock: Clock[IO] = zeroClock
  implicit val ioRuntime: IORuntime = IORuntime.global
  private val singleThreadCtx: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  checkAll(
    "ContextualLogger",
    LoggerTests[F](
      new WriterTLogger[IO].withConstContext(Map.empty),
      _.written.evalOn(singleThreadCtx).unsafeRunSync()
    ).all
  )

  it should "add constant context to the record" in {
    forAll { (loggerMessage: LoggerMessage, ctx: Map[String, String]) =>
      val logger = new WriterTLogger[IO].withConstContext(ctx)
      val List(written) = logger.log(loggerMessage).written.unsafeRunSync(): @unchecked
      written.context shouldBe loggerMessage.context ++ ctx
    }
  }

  it should "add constant context to the records" in {
    forAll { (messages: List[LoggerMessage], ctx: Map[String, String]) =>
      val logger = new WriterTLogger[IO].withConstContext(ctx)
      val written = logger.log(messages).written.unsafeRunSync()
      written.map(_.context) shouldBe messages.map(_.context ++ ctx)
    }
  }
}
