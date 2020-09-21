package fr.maif.json;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A combinaison on JsonRead and JsonWrite.
 *
 * @param <T>
 */
public interface JsonFormat<T> extends JsonRead<T>, JsonWrite<T> {

    JsonRead<T> jsonRead();
    JsonWrite<T> jsonWrite();

    static <T> JsonFormat<T> of(JsonRead<T> read, JsonWrite<T> write) {
        return new JsonFormat<T>() {
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

}
