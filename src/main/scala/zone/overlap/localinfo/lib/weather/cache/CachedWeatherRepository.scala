// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import java.time.Instant
import com.apple.foundationdb.record.RecordMetaData
import com.apple.foundationdb.record.metadata.Index
import com.apple.foundationdb.record.metadata.Key.Expressions._
import com.apple.foundationdb.record.provider.foundationdb._
import com.apple.foundationdb.record.provider.foundationdb.keyspace.{KeySpace, KeySpaceDirectory}
import com.apple.foundationdb.record.query.RecordQuery
import com.apple.foundationdb.record.query.expressions.Query
import com.apple.foundationdb.tuple.Tuple
import com.google.protobuf.Message
import monix.eval.Task
import zone.overlap.localinfo.persistence.{CachedWeatherProto, CachedWeather => CachedWeatherJava}
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather

class CachedWeatherRepository(db: FDBDatabase, keySpaceDirectoryName: String) {

  private lazy val recordMetaData = {
    val metaDataBuilder = RecordMetaData.newBuilder().setRecords(CachedWeatherProto.getDescriptor)
    val cachedAtIndex = new Index(
      "cached_at_index",
      field("cached_at").nest("seconds")
    )
    metaDataBuilder.addIndex("CachedWeather", cachedAtIndex)
    metaDataBuilder.build()
  }

  private lazy val keySpacePath = {
    val keySpace = new KeySpace(
      new KeySpaceDirectory(keySpaceDirectoryName, KeySpaceDirectory.KeyType.STRING, keySpaceDirectoryName)
    )
    keySpace.path(keySpaceDirectoryName)
  }

  private val getRecordStore = (context: FDBRecordContext) =>
    FDBRecordStore
      .newBuilder()
      .setMetaDataProvider(recordMetaData)
      .setContext(context)
      .setKeySpacePath(keySpacePath)
      .createOrOpen();

  def get(localityKey: String): Task[Option[CachedWeather]] = {
    Task {
      Option[FDBStoredRecord[Message]](
        db.run(
          getRecordStore(_).loadRecord(Tuple.from(localityKey))
        ))
        .map(toCachedWeather(_))
    }
  }

  def save(cachedWeather: CachedWeather): Task[Unit] = {
    Task[FDBStoredRecord[Message]] {
      db.run(
        getRecordStore(_).saveRecord(CachedWeather.toJavaProto(cachedWeather))
      )
    } map (_ => ())
  }

  def deleteOlderThan(instant: Instant): Task[Unit] = {
    Task {
      db.run { context =>
        val store = getRecordStore(context)

        val query = RecordQuery
          .newBuilder()
          .setRecordType("CachedWeather")
          .setFilter(Query.field("cached_at") matches {
            Query.field("seconds") lessThan instant.getEpochSecond
          })
          .build()

        store
          .executeQuery(query)
          .forEach(r => store.deleteRecord(r.getPrimaryKey))
          .join()

        null // Record Layer is a Java library and returning null is a thing in Javaland
      }
    }
  }

  private def toCachedWeather(storedRecord: FDBStoredRecord[Message]): CachedWeather = {
    CachedWeather.fromJavaProto(
      CachedWeatherJava
        .newBuilder()
        .mergeFrom(storedRecord.getRecord())
        .build()
    )
  }
}
