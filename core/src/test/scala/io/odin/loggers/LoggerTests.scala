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

import io.odin.{Level, Logger, LoggerMessage}

import cats.{Eq, Monad}
import cats.laws.discipline.*
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait LoggerTests[F[_]] extends Laws {

  def loggerLaws: LoggerLaws[F]
  def logger: Logger[F]

  def all(implicit arbMsg: Arbitrary[LoggerMessage], arbLvl: Arbitrary[Level], eqF: Eq[List[LoggerMessage]]): RuleSet =
    new SimpleRuleSet(
      "logger",
      "checks minLevel" -> Prop.forAll((msg: LoggerMessage, level: Level) =>
        loggerLaws.checksMinLevel(logger, msg, level)
      ),
      "log(list) <-> list.traverse(log)" -> Prop.forAll((msgs: List[LoggerMessage]) =>
        loggerLaws.batchEqualsToTraverse(logger, msgs)
      )
    )

}

object LoggerTests {

  def apply[F[_]](l: Logger[F], extract: F[Unit] => List[LoggerMessage])(implicit monad: Monad[F]): LoggerTests[F] =
    new LoggerTests[F] {
      def loggerLaws: LoggerLaws[F] = new LoggerLaws[F] {
        val F: Monad[F]                             = monad
        val written: F[Unit] => List[LoggerMessage] = extract
      }

      def logger: Logger[F] = l
    }

}
