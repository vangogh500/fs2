package fs2

import cats.Eq
import cats.effect.IO
import cats.effect.laws.discipline.arbitrary._
import cats.effect.laws.util.TestContext
import cats.effect.laws.util.TestInstances._
import cats.implicits._
import cats.laws.discipline._

import org.scalacheck.{Arbitrary, Gen}
import Arbitrary.arbitrary

class StreamLawsSpec extends LawsSpec {
  implicit val ec: TestContext = TestContext()

  implicit def arbStream[F[_], O](implicit arbO: Arbitrary[O],
                                  arbFo: Arbitrary[F[O]],
                                  arbFu: Arbitrary[F[Unit]]): Arbitrary[Stream[F, O]] =
    Arbitrary(
      Gen.frequency(
        10 -> arbitrary[List[O]].map(os => Stream.emits(os).take(10)),
        10 -> arbitrary[List[O]].map(os => Stream.emits(os).take(10).unchunk),
        5 -> arbitrary[F[O]].map(fo => Stream.eval(fo)),
        1 -> (for {
          acquire <- arbitrary[F[O]]
          release <- arbitrary[F[Unit]]
          use <- arbStream[F, O].arbitrary
        } yield Stream.bracket(acquire)(_ => release).flatMap(_ => use))
      ))

  implicit def eqStream[O: Eq]: Eq[Stream[IO, O]] =
    Eq.instance(
      (x, y) =>
        Eq[IO[Vector[Either[Throwable, O]]]]
          .eqv(x.attempt.compile.toVector, y.attempt.compile.toVector))

  // TODO Uncomment when cats-laws supports ScalaCheck 1.14
  // checkAll("MonadError[Stream[F, ?], Throwable]",
  //          MonadErrorTests[Stream[IO, ?], Throwable].monadError[Int, Int, Int])
  // checkAll("FunctorFilter[Stream[F, ?]]",
  //          FunctorFilterTests[Stream[IO, ?]].functorFilter[String, Int, Int])
  // checkAll("MonoidK[Stream[F, ?]]", MonoidKTests[Stream[IO, ?]].monoidK[Int])
}
