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
import scala.concurrent.ExecutionContext

import io.odin.*
import io.odin.syntax.*

import cats.data.WriterT
import cats.effect.{Clock, IO}
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*

class FilterLoggerSpec extends OdinSpec {

  type F[A] = WriterT[IO, List[LoggerMessage], A]
  implicit val clock: Clock[IO]                 = zeroClock
  implicit val ioRuntime: IORuntime             = IORuntime.global
  private val singleThreadCtx: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  checkAll(
    "FilterLogger",
    LoggerTests[F](
      new WriterTLogger[IO].filter(_.exception.isDefined),
      _.written.evalOn(singleThreadCtx).unsafeRunSync()
    ).all
  )

  it should "logger.filter(p).log(msg) <-> F.whenA(p)(log(msg))" in {
    forAll { (msgs: List[LoggerMessage], p: LoggerMessage => Boolean) =>
      {
        val logger       = new WriterTLogger[IO].filter(p)
        val written      = msgs.traverse(logger.log).written.unsafeRunSync()
        val batchWritten = logger.log(msgs).written.unsafeRunSync()

        written shouldBe msgs.filter(p)
        batchWritten shouldBe written
      }
    }
  }

}
