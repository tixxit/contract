package net.tixxit.contract
package hash

import java.util.UUID

import scala.annotation.tailrec

import org.apache.commons.codec.binary.Base64

import net.tixxit.contract.util._

object MD5HashStream extends HashStream {
  private final val MaxTries = 8
  private final val InitLength = 3

  // Convert a 128 bit integer to an array of 16 bytes in LE.
  private def toBytes(low: Long, high: Long): Array[Byte] = {
    val bytes = new Array[Byte](16)
    @tailrec def fill(i: Int, n: Long): Unit =
      if (n != 0) {
        bytes(i) = (n & 255L).toByte
        fill(i + 1, n >>> 8)
      }
    fill(0, low)
    fill(8, high)
    bytes
  }

  // Uses MD5 to compute a hash of n bytes.
  private def hash(n: Int, bytes: Array[Byte]): Array[Byte] = {
    require(n <= 16)
    val uuid = UUID.nameUUIDFromBytes(bytes)
    val bighash = toBytes(uuid.getLeastSignificantBits, uuid.getMostSignificantBits)
    val hash = new Array[Byte](n)
    @tailrec def fill(i: Int): Unit = if (i < bighash.length) {
      val j = i % hash.length
      hash(j) = (hash(j) ^ bighash(i)).toByte
      fill(i + 1)
    }
    fill(0)
    hash
  }

  // An infinite stream of hashed values.
  private def hashStream(value: String, tries: Int, width: Int): InfStream[String] = {
    if (width < 16 && tries > MaxTries) {
      hashStream(value, 0, width + 1)
    } else {
      val bytes = hash(width, (value + tries.toString).getBytes(Charsets.Utf8))
      InfStream(Base64.encodeBase64URLSafeString(bytes), hashStream(value, tries + 1, width))
    }
  }

  def hashStream(value: String): InfStream[String] =
    hashStream(value, 0, InitLength)
}
