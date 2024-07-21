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

package io.odin.config

import alleycats.std.iterable._
import cats.Monad
import cats.effect.kernel.Clock
import cats.syntax.all._
import io.odin.loggers.DefaultLogger
import io.odin.{Level, Logger, LoggerMessage}

import scala.annotation.tailrec

private[config] class EnclosureRouting[F[_]: Clock](fallback: Logger[F], router: List[(String, Logger[F])])(
    implicit F: Monad[F]
) extends DefaultLogger(Level.Trace) {
  private val indexedRouter = router.mapWithIndex {
    case ((packageName, logger), idx) =>
      (packageName, (idx, logger))
  }

  def submit(msg: LoggerMessage): F[Unit] = recLog(indexedRouter, msg)

  override def submit(msgs: List[LoggerMessage]): F[Unit] = {
    val grouped: Iterable[((Int, Logger[F]), List[LoggerMessage])] = msgs
      .foldLeft(Map.empty[(Int, Logger[F]), List[LoggerMessage]]) {
        case (map, msg) =>
          val kv = indexedRouter
            .collectFirst {
              case (key, indexedLogger) if msg.position.enclosureName.startsWith(key) => indexedLogger
            }
            .getOrElse(-1 -> fallback) -> List(msg)

          map |+| Map(kv)
      }

    grouped.traverse_ {
      case ((_, logger), ms) =>
        logger.log(ms.filter(_.level >= logger.minLevel))
    }
  }

  @tailrec
  private def recLog(router: List[(String, (Int, Logger[F]))], msg: LoggerMessage): F[Unit] = router match {
    case Nil =>
      if (msg.level >= fallback.minLevel) fallback.log(msg)
      else F.unit
    case (key, (_, logger)) :: _ if msg.position.enclosureName.startsWith(key) =>
      F.whenA(msg.level >= logger.minLevel)(logger.log(msg))
    case _ :: tail => recLog(tail, msg)
  }

  def withMinimalLevel(level: Level): Logger[F] =
    new EnclosureRouting[F](
      fallback.withMinimalLevel(level),
      router.map {
        case (route, logger) =>
          route -> logger.withMinimalLevel(level)
      }
    )
}
