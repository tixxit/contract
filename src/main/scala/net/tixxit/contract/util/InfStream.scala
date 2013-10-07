package net.tixxit.contract
package util

/**
 * A simple infinite stream. It will never end and the tail is not memoized,
 * but recomputed each time it is accessed.
 */
final class InfStream[A](val head: A, tail0: => InfStream[A]) {
  def tail = tail0 // We could use a lazy val, but wastes space.

  def ::(h: A): InfStream[A] = new InfStream(h, this)

  def foldRight[Z](z: => Z)(f: (A, => Z) => Z): Z =
    f(head, tail.foldRight(z)(f))

  def toStream: Stream[A] = foldRight(Stream.empty[A]) { (a, tail) =>
    Stream.cons(a, tail)
  }

  override def toString: String =
    s"InfStream(${head.toString}, ...)"
}

object InfStream {
  def apply[A](head: A, tail: => InfStream[A]) = new InfStream(head, tail)
}
