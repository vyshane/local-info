// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import java.time.Instant

import com.apple.foundationdb.record.RecordMetaData
import com.apple.foundationdb.record.metadata.{Index, Key}
import com.apple.foundationdb.record.provider.foundationdb.{FDBDatabase, FDBRecordContext, FDBRecordStore, FDBStoredRecord}
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpace
import com.apple.foundationdb.tuple.Tuple
import com.google.protobuf.Message
import monix.eval.Task
import zone.overlap.localinfo.persistence.CachedWeather
import zone.overlap.localinfo.v1.LocalInfoProto

class CachedWeatherRepository(db: FDBDatabase, keySpace: KeySpace) {

  val recordMetaData = {
    val metaDataBuilder = RecordMetaData.newBuilder().setRecords(LocalInfoProto.javaDescriptor)
    metaDataBuilder
      .getRecordType("CachedWeather")
      .setPrimaryKey(Key.Expressions.field("locality_key"))
    metaDataBuilder
      .addIndex("CachedWeather", new Index("retrievedAtIndex", Key.Expressions.field("retrieved_at")))
    metaDataBuilder.build()
  }

  val getRecordStore = (context: FDBRecordContext) =>
    FDBRecordStore
      .newBuilder()
      .setMetaDataProvider(recordMetaData)
      .setContext(context)
      .setKeySpacePath(keySpace.path("cached-weather"))
      .createOrOpen();

  def get(locationKey: String): Task[Option[CachedWeather]] = {
    Task {
      Option[FDBStoredRecord[Message]] {
        db.run(context => {
          getRecordStore(context).loadRecord(Tuple.from(locationKey))
        })
      } map { sr =>
        CachedWeather()
          .mergeFrom(sr.getRecord().toByteString.newCodedInput())
      }
    }
  }

  def save(cachedWeather: CachedWeather): Task[Unit] = {
    ???
  }

  def delete(locationKey: String, olderThan: Instant): Task[Boolean] = {
    ???
  }
}
