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

import io.odin.meta.Position

import cats.Eval

/**
  * Final log message that contains all the possible information to render
  *
  * @param level log level of the message
  * @param message string message
  * @param context some MDC
  * @param exception exception if exists
  * @param position origin of log
  * @param threadName current thread name
  * @param timestamp Epoch time in milliseconds at the moment of log
  */
case class LoggerMessage(
    level: Level,
    message: Eval[String],
    context: Map[String, String],
    exception: Option[Throwable],
    position: Position,
    threadName: String,
    timestamp: Long
)
