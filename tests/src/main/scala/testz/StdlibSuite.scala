/*
 * Copyright (c) 2018, Edmund Noble
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package testz

import runner.TestOutput

object StdlibSuite {

  def tests[T](harness: Harness[T]): T = {
    import harness._

    val noOpHarness = PureHarness.makeFromPrinterR((_, _) => ())

    namedSection("PureHarness")(
      test("section") { () =>
        def test(faileds: List[Boolean], expected: Boolean): Result = {
          val arr = Array.fill(faileds.length)("")
          def setter(r: List[Int], ls: List[String], i: Int): () => Unit =
            () => arr(i) += (s"""${ls.mkString(", ")} - ${r.mkString(", ")}""" + i)
          val outputs = faileds.zipWithIndex.map {
            case (f, i) => (r: List[Int], ls: List[String]) => new TestOutput(f, setter(r, ls, i))
          }
          val combined =
            noOpHarness.section[List[Int]](outputs.head, outputs.tail: _*)(List(1, 2), List("hey", "there"))
          combined.print()
          assert(
            (combined.failed == expected) &&
            arr.zipWithIndex.forall {
              case (s, i) => (s == ("hey, there - 1, 2" + i))
            }
          )
        }

        List(
          test(List(false, false, false, false, false), false),
          test(List(true, true, true, true, true), true),
          test(List(true, true, true, true, false), true),
          test(List(false, false, false, false, true), true),
          test(List(true, false, false, false, false), true),
          test(List(false, true, true, true, true), true)
        ).reduce(Result.combine)
      },
      test("namedSection") { () =>
        var x = ""
        var y = ""
        val testX: PureHarness.Uses[List[Int]] =
          (r, ls) => new TestOutput(false, () => x = s"$x - $ls - $r")
        val testY: PureHarness.Uses[List[Int]] =
          (r, ls) => new TestOutput(false, () => y = s"$y - $ls - $r")
        val sec =
          noOpHarness.namedSection("section name")(testX, testY)
        val result = sec(List(1, 2, 3), List("outer"))
        result.print()
        assert(
          (result.failed == false) &&
          x == s""" - List(section name, outer) - List(1, 2, 3)""" &&
          y == s""" - List(section name, outer) - List(1, 2, 3)"""
        )
      },
      test("test") { () =>
        var outerRes = ""
        var x = ""
        val harness = PureHarness.makeFromPrinterR((r, ls) => x = s"$x - $r - $ls")
        val test =
          harness.test[List[Int]]("test name") { (res: List[Int]) =>
            outerRes = res.toString
            Succeed()
          }
        val result = test(List(1, 2), List("outer"))
        val notPrintedEarly = (x == "")
        result.print()
        assert(
          notPrintedEarly &&
          (x == " - Succeed - List(test name, outer)") &&
          (outerRes == "List(1, 2)")
        )
      },
    )

  }
}
