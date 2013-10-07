package net.tixxit.contract
package store

import java.io.File

import com.sleepycat.je.{ Environment, Database, EnvironmentConfig, DatabaseConfig,
                          DatabaseEntry, LockMode, OperationStatus, Durability }

import scalaz.effect.IO

final class BerkeleyDBStore(env: Environment, db: Database) extends Store[IO] {
  def putIfAbsent(key: Array[Byte], value: Array[Byte]): IO[Option[Array[Byte]]] = IO {
    val dbKey = new DatabaseEntry(key)
    val dbVal = new DatabaseEntry(value)
    val txn = env.beginTransaction(null, null)
    val status = db.putNoOverwrite(txn, dbKey, dbVal)
    txn.commit()

    status match {
      case OperationStatus.SUCCESS =>
        None
      case OperationStatus.KEYEXIST =>
        db.get(null, dbKey, dbVal, LockMode.READ_UNCOMMITTED) // Assume success.
        Some(dbVal.getData())
      case status =>
        throw new IllegalStateException(s"Unexpected return value from BDB: $status")
    }
  }

  def get(key: Array[Byte]): IO[Option[Array[Byte]]] = IO {
    val dbKey = new DatabaseEntry(key)
    val dbVal = new DatabaseEntry()
    db.get(null, dbKey, dbVal, LockMode.READ_UNCOMMITTED) match {
      case OperationStatus.SUCCESS => Some(dbVal.getData())
      case OperationStatus.NOTFOUND => None
      case status =>
        throw new IllegalStateException(s"Unexpected return value from BDB: $status")
    }
  }

  def close(): IO[Unit] = IO {
    db.synchronized(db.close())
    env.synchronized(env.close())
  }
}

object BerkeleyDBStore {
  def apply(file: File, name: String): Store[IO] = {
    val env = new Environment(file, new EnvironmentConfig()
      .setTransactional(true)
      .setAllowCreate(true))
    val txn = env.beginTransaction(null, null);
    val db = env.openDatabase(txn, name, new DatabaseConfig()
      .setTransactional(true)
      .setAllowCreate(true))
    txn.commit()
    new BerkeleyDBStore(env, db)
  }
}
