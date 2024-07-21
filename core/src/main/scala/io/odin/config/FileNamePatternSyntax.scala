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

import java.time.LocalDateTime

trait FileNamePattern {
  def extract(dateTime: LocalDateTime): String
}

trait FileNamePatternSyntax {

  case class Value(value: String) extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = value
  }

  case object year extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getYear)
  }

  case object month extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getMonthValue)
  }

  case object day extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getDayOfMonth)
  }

  case object hour extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getHour)
  }

  case object minute extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getMinute)
  }

  case object second extends FileNamePattern {
    def extract(dateTime: LocalDateTime): String = padWithZero(dateTime.getSecond)
  }

  private[odin] def padWithZero(value: Int): String = f"$value%02d"

}
