package net.tixxit.contract
package store

/**
 * An abstraction over key/value stores.
 */
trait Store[M[_]] {

  /**
   * Store `value` at `key` if no value already exists there. If it does exist,
   * then the existing value should be returned instead. This should be atomic.
   */
  def putIfAbsent(key: Array[Byte], value: Array[Byte]): M[Option[Array[Byte]]]

  /**
   * Returns the value associated with `key` if it exists.
   */
  def get(key: Array[Byte]): M[Option[Array[Byte]]]

  /**
   * Close the `Store` and free any used resources.
   */
  def close(): M[Unit]
}
