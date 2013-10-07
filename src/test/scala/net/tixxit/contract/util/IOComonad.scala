package net.tixxit.contract.util

import scalaz._
import scalaz.effect._

final object IOComonad extends Comonad[IO] {
  def copoint[A](p: IO[A]): A = p.unsafePerformIO
  def cobind[A, B](fa: IO[A])(f: IO[A] => B): IO[B] = IO(f(fa))
  def map[A, B](fa: IO[A])(f: A => B): IO[B] = Functor[IO].map(fa)(f)
  def cojoin[A](a: IO[A]): IO[IO[A]] = IO(a)
}

