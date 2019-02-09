// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import java.time.Instant

import com.apple.foundationdb.record.RecordMetaData
import com.apple.foundationdb.record.metadata.expressions.KeyExpression.FanType
import com.apple.foundationdb.record.metadata.Index
import com.apple.foundationdb.record.metadata.Key.Expressions._
import com.apple.foundationdb.record.provider.foundationdb.{FDBDatabase, FDBRecordContext, FDBRecordStore, FDBStoredRecord}
import com.apple.foundationdb.record.provider.foundationdb.keyspace.{KeySpace, KeySpaceDirectory}
import com.apple.foundationdb.tuple.Tuple
import com.google.protobuf.Message
import monix.eval.Task
import zone.overlap.localinfo.persistence.{CachedWeatherProto, CachedWeather => CachedWeatherJava}
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather

class CachedWeatherRepository(db: FDBDatabase, keySpaceDirectoryName: String) {

  private val recordMetaData = {
    val metaDataBuilder = RecordMetaData.newBuilder().setRecords(CachedWeatherProto.getDescriptor)
    metaDataBuilder.addIndex("CachedWeather",
      new Index(
        "retrieved_at_index",
        field("retrieved_at").nest(concat(field("seconds"), field("nanos")))
      )
    )
    metaDataBuilder.build()
  }

  val keySpacePath = {
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

  def get(locationKey: String): Task[Option[CachedWeather]] = {
    Task {
      Option[FDBStoredRecord[Message]] {
        db.run(context => {
          getRecordStore(context).loadRecord(Tuple.from(locationKey))
        })
      } map toCachedWeather
    }
  }

  def save(cachedWeather: CachedWeather): Task[Unit] = {
    Task[FDBStoredRecord[Message]] {
      db.run(context => {
        getRecordStore(context).saveRecord(CachedWeather.toJavaProto(cachedWeather))
      })
    } map (_ => ())
  }

  def delete(locationKey: String, olderThan: Instant): Task[Boolean] = {
    ???
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
