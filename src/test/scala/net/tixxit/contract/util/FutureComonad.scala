package net.tixxit.contract.util

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration

import scalaz._

final class FutureComonad(duration: FiniteDuration)(implicit ec: ExecutionContext)
extends Comonad[Future] {
  def copoint[A](p: Future[A]): A = Await.result(p, duration)
  def cobind[A, B](fa: Future[A])(f: Future[A] => B): Future[B] = Future.successful(f(fa))
  def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa map f
  def cojoin[A](a: Future[A]): Future[Future[A]] = Future.successful(a)
}

