package net.tixxit.contract
package hash

import net.tixxit.contract.util._

trait HashStream {
  /** Given a string value, produce an infinite stream of hashes. */
  def hashStream(value: String): InfStream[String]
}
