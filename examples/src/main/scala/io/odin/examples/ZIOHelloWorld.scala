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

import io.odin.zio.*
import io.odin.Logger

import zio.*

object ZIOHelloWorld extends App {

  val logger: Logger[IO[LoggerError, *]] = consoleLogger()(this)

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    logger.info("Hello world").fold(_ => ExitCode.failure, _ => ExitCode.success)

}
