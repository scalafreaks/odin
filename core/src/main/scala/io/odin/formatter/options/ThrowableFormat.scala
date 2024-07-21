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

package io.odin.formatter.options

final case class ThrowableFormat(
    depth: ThrowableFormat.Depth,
    indent: ThrowableFormat.Indent,
    filter: ThrowableFormat.Filter
)

object ThrowableFormat {

  val Default: ThrowableFormat = ThrowableFormat(Depth.Full, Indent.NoIndent, Filter.NoFilter)

  sealed trait Depth
  object Depth {
    case object Full extends Depth
    final case class Fixed(size: Int) extends Depth
  }

  sealed trait Indent
  object Indent {
    case object NoIndent extends Indent
    final case class Fixed(size: Int) extends Indent
  }

  sealed trait Filter
  object Filter {
    case object NoFilter extends Filter
    final case class Excluding(prefixes: Set[String]) extends Filter

    object Excluding {
      def apply(prefixes: String*): Excluding = new Excluding(prefixes.toSet)
    }
  }

}
