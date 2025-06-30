package fr.maif.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.collection.List;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A combinaison on JsonRead and JsonWrite.
 *
 * @param <T>
 */
public interface JsonFormat<T> extends JsonRead<T>, JsonWrite<T> {

    JsonRead<T> jsonRead();
    JsonWrite<T> jsonWrite();

    static <T> JsonFormat<T> of(JsonRead<T> read, JsonWrite<T> write) {
        return new JsonFormat<>() {
            @Override
            public JsonRead<T> jsonRead() {
                return read;
            }

            @Override
            public JsonWrite<T> jsonWrite() {
                return write;
            }

            @Override
            public JsResult<T> read(JsonNode jsonNode) {
                return read.read(jsonNode);
            }

            @Override
            public JsonSchema jsonSchema() {
                return read.jsonSchema();
            }

            @Override
            public JsonNode write(T value) {
                return write.write(value);
            }
        };
    }

    static <T> JsonFormat<T> _$fromClass(Class<T> clazz) {
        return JsonFormat.of(JsonRead._fromClass(clazz), JsonWrite.auto());
    }

    static <T> JsonFormat<T> _$fromClass(TypeReference<T> clazz) {
        return JsonFormat.of(JsonRead._fromClass(clazz), JsonWrite.auto());
    }

    static <T> JsonFormat<List<T>> _$list(JsonFormat<T> format) {
        return JsonFormat.of(JsonRead._list(format), JsonWrite.$list(format));
    }

    static <T> JsonFormat<ArrayNode> _$jsonArray() {
        return JsonFormat.of(JsonRead._jsonArray(), JsonWrite.$jsonArray());
    }

    static <T> JsonFormat<ObjectNode> _$jsonObject() {
        return JsonFormat.of(JsonRead._jsonObject(), JsonWrite.$jsonObject());
    }

    static <T> JsonFormat<JsonNode> _$json() {
        return JsonFormat.of(JsonRead._json(), it -> it);
    }

    static JsonFormat<String> _$string() {
        return JsonFormat.of(JsonRead._string(), JsonWrite.$string());
    }

    static JsonFormat<Integer> _$int() {
        return JsonFormat.of(JsonRead._int(), JsonWrite.$int());
    }

    static JsonFormat<Long> _$long() {
        return JsonFormat.of(JsonRead._long(), JsonWrite.$long());
    }

    static JsonFormat<Double> _$double() {
        return JsonFormat.of(JsonRead._double(), JsonWrite.$double());
    }

    static JsonFormat<Float> _$float() {
        return JsonFormat.of(JsonRead._float(), JsonWrite.$float());
    }

    static JsonFormat<BigDecimal> _$bigDecimal() {
        return JsonFormat.of(JsonRead._bigDecimal(), JsonWrite.$bigdecimal());
    }

    static JsonFormat<LocalDate> _$localDate(DateTimeFormatter formatter) {
        return JsonFormat.of(JsonRead._localDate(formatter), JsonWrite.$localdate(formatter));
    }

    static JsonFormat<LocalDate> _$isoLocalDate() {
        return JsonFormat.of(JsonRead._isoLocalDate(), JsonWrite.$localdate());
    }

    static JsonFormat<LocalDateTime> _$localDateTime(DateTimeFormatter formatter) {
        return JsonFormat.of(JsonRead._localDateTime(formatter), JsonWrite.$localdatetime(formatter));
    }

    static JsonFormat<LocalDateTime> _$isoLocalDateTime() {
        return JsonFormat.of(JsonRead._isoLocalDateTime(), JsonWrite.$localdatetime());
    }

    static JsonFormat<Boolean> _$boolean() {
        return JsonFormat.of(JsonRead._boolean(), JsonWrite.$boolean());
    }

    static <E extends Enum<E>> JsonFormat<E> _$enum(Class<E> clazz) {
        return JsonFormat.of(JsonRead._enum(clazz), JsonWrite.$enum());
    }

}
