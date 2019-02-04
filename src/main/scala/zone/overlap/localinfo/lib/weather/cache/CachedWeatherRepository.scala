// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import java.time.Instant

import com.apple.foundationdb.record.RecordMetaData
import com.apple.foundationdb.record.metadata.{Index, Key}
import com.apple.foundationdb.record.provider.foundationdb.{FDBDatabase, FDBRecordContext, FDBRecordStore, FDBStoredRecord}
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpace
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpaceDirectory
import com.apple.foundationdb.tuple.Tuple
import com.google.protobuf.Message
import monix.eval.Task
import zone.overlap.localinfo.persistence.CachedWeather
import zone.overlap.localinfo.v1.LocalInfoProto

class CachedWeatherRepository(db: FDBDatabase) {

  val keySpace = new KeySpace(new KeySpaceDirectory("local-info", KeySpaceDirectory.KeyType.STRING, "local-info"))
  val path = keySpace.path("cached-weather")
  val metaDataBuilder = RecordMetaData.newBuilder().setRecords(LocalInfoProto.javaDescriptor)

  metaDataBuilder
    .getRecordType("CachedWeather")
    .setPrimaryKey(Key.Expressions.field("locality_key"))

  metaDataBuilder
    .addIndex("CachedWeather", new Index("retrievedAtIndex", Key.Expressions.field("retrieved_at")))

  val recordMetaData = metaDataBuilder.build()

  val recordStoreProvider = (context: FDBRecordContext) =>
    FDBRecordStore
      .newBuilder()
      .setMetaDataProvider(recordMetaData)
      .setContext(context)
      .setKeySpacePath(path)
      .createOrOpen();

  def get(locationKey: String): Task[Option[CachedWeather]] = {
    Task {
      Option[FDBStoredRecord[Message]](
        db.run(context => {
          recordStoreProvider
            .apply(context)
            .loadRecord(Tuple.from(locationKey))
        })
      ).map(r => CachedWeather().mergeFrom(r.getRecord().toByteString.newCodedInput()))
    }
  }

  def save(cachedWeather: CachedWeather): Task[Unit] = {
    ???
  }

  def delete(locationKey: String, olderThan: Instant): Task[Boolean] = {
    ???
  }
}
