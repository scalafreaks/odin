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

package io.odin.extras.loggers

import io.odin.{Level, Logger, LoggerMessage, OdinSpec}
import io.odin.extras.syntax.*
import io.odin.loggers.{DefaultLogger, HasContext}
import io.odin.syntax.*

import cats.data.Kleisli
import cats.effect.{IO, Sync}
import cats.effect.kernel.Ref
import cats.effect.unsafe.IORuntime
import cats.syntax.applicativeError.*
import cats.syntax.flatMap.*
import cats.syntax.order.*

class ConditionalLoggerSpec extends OdinSpec {

  type F[A] = Kleisli[IO, Map[String, String], A]

  case class RefLogger(ref: Ref[F, List[LoggerMessage]], override val minLevel: Level = Level.Trace)
      extends DefaultLogger[F](minLevel) {

    def submit(msg: LoggerMessage): F[Unit] = ref.update(_ :+ msg)

    def withMinimalLevel(level: Level): Logger[F] = copy(minLevel = level)

  }

  private implicit val ioRuntime: IORuntime                        = IORuntime.global
  private implicit val hasContext: HasContext[Map[String, String]] = (env: Map[String, String]) => env

  it should "use log level of the inner logger in case of success" in {
    forAll { (messages: List[LoggerMessage], ctx: Map[String, String]) =>
      val fa =
        for {
          ref <- Ref.of[F, List[LoggerMessage]](List.empty)
          _   <- RefLogger(ref)
                 .withMinimalLevel(Level.Info)
                 .withContext
                 .withErrorLevel(Level.Debug)(logger => logger.log(messages))
          written <- ref.get
        } yield written

      val written  = fa.run(ctx).unsafeRunSync()
      val expected = messages.filter(_.level >= Level.Info).map(m => m.copy(context = m.context ++ ctx))

      written shouldBe expected
    }
  }

  it should "use log level of the conditional logger in case of error" in {
    forAll { (messages: List[LoggerMessage], ctx: Map[String, String]) =>
      val error = new RuntimeException("Boom")

      val fa =
        for {
          ref     <- Ref.of[F, List[LoggerMessage]](List.empty)
          attempt <- RefLogger(ref)
                       .withMinimalLevel(Level.Info)
                       .withContext
                       .withErrorLevel(Level.Debug)(logger => logger.log(messages) >> Sync[F].raiseError[Unit](error))
                       .attempt
          written <- ref.get
        } yield (attempt, written)

      val (attempt, written) = fa.run(ctx).unsafeRunSync()
      val expected           = messages.filter(_.level >= Level.Debug).map(m => m.copy(context = m.context ++ ctx))

      attempt shouldBe Left(error)
      written shouldBe expected
    }
  }

}
