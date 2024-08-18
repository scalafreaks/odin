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

package io.odin

import io.odin.loggers.*
import io.odin.meta.Render

import cats.effect.kernel.{Async, Clock, Resource}
import cats.effect.std.Dispatcher
import cats.Monad

package object syntax {

  implicit class LoggerSyntax[F[_]](logger: Logger[F]) {

    /**
      * Create logger that adds constant context to each log record
      * @param ctx constant context
      */
    def withConstContext(ctx: Map[String, String])(implicit clock: Clock[F], monad: Monad[F]): Logger[F] =
      ConstContextLogger.withConstContext(ctx, logger)

    /**
      * Create contextual logger that is capable of picking up context from inside of `F[_]`.
      * See `ContextualLogger` for more info
      */
    def withContext(implicit clock: Clock[F], monad: Monad[F], withContext: WithContext[F]): Logger[F] =
      ContextualLogger.withContext(logger)

    /**
      * Create async logger that buffers the messages up to the limit (if any) and flushes it down the chain asynchronously
      * @param maxBufferSize max logs buffer size
      * @return Logger suspended in `Resource`. Once this `Resource` started, internal flush loop is initialized. Once
      *         resource is released, flushing is also stopped.
      */
    def withAsync(maxBufferSize: Option[Int] = None)(implicit F: Async[F]): Resource[F, Logger[F]] =
      AsyncLogger.withAsync(logger, maxBufferSize)

    /**
      * Create and unsafely run async logger that buffers the messages up to the limit (if any) and
      * flushes it down the chain asynchronously
      * @param maxBufferSize max logs buffer size
      */
    def withAsyncUnsafe(maxBufferSize: Option[Int] = None)(implicit F: Async[F], dispatcher: Dispatcher[F]): Logger[F] =
      AsyncLogger.withAsyncUnsafe(logger, maxBufferSize)

    /**
      * Modify logger message before it's written to the logger
      */
    def contramap(f: LoggerMessage => LoggerMessage)(implicit clock: Clock[F], F: Monad[F]): Logger[F] =
      ContramapLogger.withContramap(f, logger)

    /**
      * Filter messages given the predicate. Falsified cases are dropped from the logging
      */
    def filter(f: LoggerMessage => Boolean)(implicit clock: Clock[F], F: Monad[F]): Logger[F] =
      FilterLogger.withFilter(f, logger)

    /**
      * Create logger that hashes context value given that context key matches one of the arguments
      */
    def withSecretContext(
        key: String,
        keys: String*
    )(implicit clock: Clock[F], monad: Monad[F]): Logger[F] =
      logger.contramap(SecretLogger(Set(key) ++ keys))

  }

  /**
    * Syntax for loggers suspended in `Resource` (i.e. `AsyncLogger` or `FileLogger`)
    */
  implicit class ResourceLoggerSyntax[F[_]](resource: Resource[F, Logger[F]]) {

    /**
      * Create async logger that buffers the messages up to the limit (if any) and flushes it down the chain asynchronously
      * @param maxBufferSize max logs buffer size
      * @return Logger suspended in `Resource`. Once this `Resource` started, internal flush loop is initialized. Once
      *         resource is released, flushing is also stopped.
      */
    def withAsync(maxBufferSize: Option[Int] = None)(implicit F: Async[F]): Resource[F, Logger[F]] =
      resource.flatMap(AsyncLogger.withAsync(_, maxBufferSize))

    /**
      * Create logger that adds constant context to each log record
      * @param ctx constant context
      */
    def withConstContext(ctx: Map[String, String])(implicit clock: Clock[F], monad: Monad[F]): Resource[F, Logger[F]] =
      resource.map(ConstContextLogger.withConstContext(ctx, _))

    /**
      * Create contextual logger that is capable of picking up context from inside of `F[_]`.
      * See `ContextualLogger` for more info
      */
    def withContext(implicit clock: Clock[F], monad: Monad[F], withContext: WithContext[F]): Resource[F, Logger[F]] =
      resource.map(ContextualLogger.withContext[F])

    /**
      * Intercept logger message before it's written to the logger
      */
    def contramap(f: LoggerMessage => LoggerMessage)(implicit clock: Clock[F], F: Monad[F]): Resource[F, Logger[F]] =
      resource.map(ContramapLogger.withContramap(f, _))

    /**
      * Filter messages given the predicate. Falsified cases are dropped from the logging
      */
    def filter(f: LoggerMessage => Boolean)(implicit clock: Clock[F], F: Monad[F]): Resource[F, Logger[F]] =
      resource.map(FilterLogger.withFilter(f, _))

    /**
      * Create logger that hashes context value given that context key matches one of the arguments
      */
    def withSecretContext(
        key: String,
        keys: String*
    )(implicit timer: Clock[F], monad: Monad[F]): Resource[F, Logger[F]] =
      resource.map(logger => logger.contramap(SecretLogger(Set(key) ++ keys)))

  }

  implicit class RenderInterpolator(private val sc: StringContext) extends AnyVal {

    def render(args: Render.Rendered*): String = sc.s(args*)

  }

}
