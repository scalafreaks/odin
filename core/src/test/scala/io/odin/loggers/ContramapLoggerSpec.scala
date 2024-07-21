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
import cats.syntax.all._
import io.odin.syntax._
import io.odin.{LoggerMessage, OdinSpec}

import scala.concurrent.ExecutionContext

class ContramapLoggerSpec extends OdinSpec {
  type F[A] = WriterT[IO, List[LoggerMessage], A]
  implicit val clock: Clock[IO] = zeroClock
  implicit val ioRuntime: IORuntime = IORuntime.global
  private val singleThreadCtx: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  checkAll(
    "ContramapLogger",
    LoggerTests[F](new WriterTLogger[IO].contramap(identity), _.written.evalOn(singleThreadCtx).unsafeRunSync()).all
  )

  it should "contramap(identity).log(msg) <-> log(msg)" in {
    val logger = new WriterTLogger[IO].contramap(identity)
    forAll { (msgs: List[LoggerMessage]) =>
      val written = msgs.traverse(logger.log).written.unsafeRunSync()
      val batchWritten = logger.log(msgs).written.unsafeRunSync()

      written shouldBe msgs
      batchWritten shouldBe written
    }
  }

  it should "contramap(f).log(msg) <-> log(f(msg))" in {
    forAll { (msgs: List[LoggerMessage], fn: LoggerMessage => LoggerMessage) =>
      val logger = new WriterTLogger[IO].contramap(fn)
      val written = msgs.traverse(logger.log).written.unsafeRunSync()
      val batchWritten = logger.log(msgs).written.unsafeRunSync()

      written shouldBe msgs.map(fn)
      batchWritten shouldBe written
    }
  }
}
