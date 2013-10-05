package net.tixxit.contract
package store

import java.io.File

import com.sleepycat.je.{ Environment, Database, EnvironmentConfig, DatabaseConfig,
                          DatabaseEntry, LockMode, OperationStatus }

import scalaz.effect.IO

final class BerkeleyDBStore(db: Database) extends Store[IO] {
  def putIfAbsent(key: Array[Byte], value: Array[Byte]): IO[Option[Array[Byte]]] = IO {
    val dbKey = new DatabaseEntry(key)
    val dbVal = new DatabaseEntry(value)
    db.putNoOverwrite(null, dbKey, dbVal) match {
      case OperationStatus.SUCCESS => None
      case OperationStatus.KEYEXIST =>
        db.get(null, dbKey, dbVal, LockMode.READ_UNCOMMITTED) // Assume success.
        Some(dbVal.getData())
    }
  }

  def get(key: Array[Byte]): IO[Option[Array[Byte]]] = IO {
    val dbKey = new DatabaseEntry(key)
    val dbVal = new DatabaseEntry()
    db.get(null, dbKey, dbVal, LockMode.READ_UNCOMMITTED) match {
      case OperationStatus.SUCCESS => Some(dbVal.getData())
      case OperationStatus.NOTFOUND => None
    }
  }

  def close(): IO[Unit] = IO(db.synchronized(db.close()))
}

object BerkeleyDBStore {
  def apply(file: File, name: String): Store[IO] = {
    val env = new Environment(file, new EnvironmentConfig()
      .setTransactional(false)
      .setAllowCreate(true))
    val db = env.openDatabase(null, name, new DatabaseConfig()
      .setAllowCreate(true))
    new BerkeleyDBStore(db)
  }
}
