// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import com.apple.foundationdb.record.RecordMetaData
import com.apple.foundationdb.record.metadata.{Index, Key}
import com.apple.foundationdb.record.provider.foundationdb.FDBDatabase
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpace
import com.apple.foundationdb.record.provider.foundationdb.keyspace.KeySpaceDirectory
import zone.overlap.localinfo.v1.LocalInfoProto

class WeatherRepository(db: FDBDatabase) {

  val keySpace = new KeySpace(new KeySpaceDirectory("local-info", KeySpaceDirectory.KeyType.STRING, "local-info"))
  val path = keySpace.path("cached-weather")
  val metaDataBuilder = RecordMetaData.newBuilder().setRecords(LocalInfoProto.javaDescriptor)

  metaDataBuilder
    .getRecordType("CachedWeather")
    .setPrimaryKey(Key.Expressions.field("changeme")); // TODO

  metaDataBuilder
    .addIndex("CachedWeather", new Index("retrievedAtIndex", Key.Expressions.field("retrieved_at")))
}
