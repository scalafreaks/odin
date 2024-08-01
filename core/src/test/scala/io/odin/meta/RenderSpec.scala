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

package io.odin.meta

import java.util.UUID
import scala.reflect.ClassTag

import io.odin.OdinSpec

import cats.data.NonEmptyList
import cats.laws.discipline.arbitrary.*
import cats.syntax.all.*
import cats.Show
import org.scalacheck.{Arbitrary, Gen}

class RenderSpec extends OdinSpec {

  it should "derive Render instance from cats.Show" in {
    val renderer = Render[Foo]
    forAll { (foo: Foo) =>
      renderer.render(foo) shouldBe foo.show
    }
  }

  it should "use .toString" in {
    val renderer = Render.fromToString[Foo]
    forAll { (foo: Foo) =>
      renderer.render(foo) shouldBe foo.toString
    }
  }

  it should "interpolate a string using Render for every argument" in {
    import io.odin.syntax.*

    implicit val intRender: Render[Int] = Render.fromToString

    forAll { (foo: Foo, string: String, int: Int) =>
      render"The interpolated $foo + $int = $string" shouldBe s"The interpolated ${foo.x} + $int = $string"
    }
  }

  behave like renderBehavior[Byte](_.toString)

  behave like renderBehavior[Short](_.toString)

  behave like renderBehavior[Int](_.toString)

  behave like renderBehavior[Long](_.toString)

  behave like renderBehavior[Double](_.toString)

  behave like renderBehavior[Float](_.toString)

  behave like renderBehavior[Boolean](_.toString)

  behave like renderBehavior[UUID](_.toString)

  behave like renderBehavior[Option[Int]](m => m.fold("None")(v => s"Some($v)"))

  behave like renderBehavior[Seq[Int]](m => m.mkString("Seq(", ", ", ")"))

  behave like renderBehavior[List[Int]](m => m.mkString("List(", ", ", ")"))

  behave like renderBehavior[Vector[Int]](m => m.mkString("Vector(", ", ", ")"))

  behave like renderBehavior[NonEmptyList[Int]](m => m.toList.mkString("NonEmptyList(", ", ", ")"))

  behave like renderBehavior[Iterable[Int]](m => m.mkString("IterableLike(", ", ", ")"))

  def renderBehavior[A: Render: ClassTag: Arbitrary](expected: A => String): Unit =
    it should s"render ${implicitly[ClassTag[A]].runtimeClass.getSimpleName}" in {
      forAll { (a: A) =>
        Render[A].render(a) shouldBe expected(a)
      }
    }

}

case class Foo(x: String)

object Foo {

  implicit val fooShow: Show[Foo] = foo => foo.x

  implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(Gen.alphaNumStr.map(Foo(_)))

}
