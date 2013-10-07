package net.tixxit.contract
package store

import scala.collection.concurrent.{ TrieMap, Map }

import scalaz._

import scalaz.syntax.monad._

final class InMemoryStore[M[+_]](implicit val M: Monad[M]) extends Store[M] {
  private final val store: Map[Vector[Byte], Vector[Byte]] = TrieMap.empty

  def putIfAbsent(key: Array[Byte], value: Array[Byte]): M[Option[Array[Byte]]] =
    M.point(store.putIfAbsent(key.toVector, value.toVector) map (_.toArray))

  def get(key: Array[Byte]): M[Option[Array[Byte]]] =
    M.point(store.get(key.toVector) map (_.toArray))

  def close(): M[Unit] = M.point(())
}
