package io.ceris.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.avro.generic.GenericRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SerdeUtil {

  private static final Map<String, String> CONVERTER_MAPPINGS = new ImmutableMap.Builder<String, String>()
      .put("io.confluent.connect.avro.AvroConverter",
          "io.confluent.kafka.serializers.KafkaAvroDeserializer")
      .put("io.confluent.connect.json.JsonSchemaConverter",
          "io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer")
      .put("org.apache.kafka.connect.json.JsonConverter",
          "io.confluent.kafka.serializers.KafkaJsonDeserializer")
      .put("org.apache.kafka.connect.converters.ByteArrayConverter",
          "org.apache.kafka.common.serialization.ByteArrayDeserializer")
      .put("org.apache.kafka.connect.converters.DoubleConverter",
          "org.apache.kafka.common.serialization.DoubleDeserializer")
      .put("org.apache.kafka.connect.converters.FloatConverter",
          "org.apache.kafka.common.serialization.FloatDeserializer")
      .put("org.apache.kafka.connect.converters.IntegerConverter",
          "org.apache.kafka.common.serialization.IntegerDeserializer")
      .put("org.apache.kafka.connect.converters.LongConverter",
          "org.apache.kafka.common.serialization.LongDeserializer")
      .put("org.apache.kafka.connect.converters.ShortConverter",
          "org.apache.kafka.common.serialization.ShortDeserializer")
      .put("org.apache.kafka.connect.storage.StringConverter",
          "org.apache.kafka.common.serialization.StringDeserializer")
      .build();

  public static String getDeserializer(String converter) {
    return Preconditions.checkNotNull(CONVERTER_MAPPINGS.get(converter),
        "Missing deserializer for %s", converter);
  }

  public static JSONObject convertToJson(Object object, String field) throws JSONException {
    if (object instanceof GenericRecord) {
      return new JSONObject(object.toString());
    } else if (object instanceof Map) {
      return new JSONObject((Map) object);
    } else {
      return new JSONObject().put(field, object);
    }
  }

  public static List<JSONObject> arrayToList(JSONArray array) {
    List<JSONObject> list = new ArrayList<>(array.length());
    for (int i = 0; i < array.length(); i++) {
      list.add(array.optJSONObject(i));
    }
    return list;
  }
}
