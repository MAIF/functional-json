package fr.maif.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vavr.collection.List;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import io.vavr.jackson.datatype.VavrModule;

import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;

public class Json {

    private static ObjectMapper defaultObjectMapper = newDefaultMapper();

    public static ObjectMapper newDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new VavrModule());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        return mapper;
    }

    /**
     * Change the underlying mapper.
     * WARNING: you should register the modules: VavrModule, Jdk8Module and JavaTimeModule
     * @param mapper the new ObjectMapper instance
     */
    public static void setMapper(ObjectMapper mapper) {
        Json.defaultObjectMapper = mapper;
    }

    /**
     * Gets the ObjectMapper used to serialize and deserialize objects to and from JSON values.
     *
     * This can be set to a custom implementation using Json.setObjectMapper.
     *
     * @return the ObjectMapper currently being used
     */
    public static ObjectMapper mapper() {
        return defaultObjectMapper;
    }


    private static String generateJson(Object o, boolean prettyPrint, boolean escapeNonASCII) {
        try {
            ObjectWriter writer = mapper().writer();
            if (prettyPrint) {
                writer = writer.with(SerializationFeature.INDENT_OUTPUT);
            }
            if (escapeNonASCII) {
                writer = writer.with(JsonGenerator.Feature.ESCAPE_NON_ASCII);
            }
            return writer.writeValueAsString(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts an object to JsonNode.
     *
     * @param data Value to convert in Json.
     * @param write a JsonWrite instance for the type.
     * @return the JSON node.
     */
    public static <T> JsonNode toJson(final T data, JsonWrite<T> write) {
        return write.write(data);
    }

    /**
     * Converts a JsonNode to a Java value
     *
     * @param <A> the type of the return value.
     * @param json Json value to convert.
     * @param jsonRead a JsonRead instance for the type.
     * @return the JsResult value.
     */
    public static <A> JsResult<A> fromJson(JsonNode json, JsonRead<A> jsonRead) {
        return jsonRead.read(json);
    }

    /**
     * Creates a new empty ObjectNode.
     * @return new empty ObjectNode.
     */
    public static ObjectNode newObject() {
        return mapper().createObjectNode();
    }

    /**
     * Creates a new empty ArrayNode.
     * @return a new empty ArrayNode.
     */
    public static ArrayNode newArray() {
        return mapper().createArrayNode();
    }

    /**
     * Converts a JsonNode to its string representation.
     * @param json    the JSON node to convert.
     * @return the string representation.
     */
    public static String stringify(JsonNode json) {
        return generateJson(json, false, false);
    }

    /**
     * Converts a JsonNode to its string representation.
     * @param json    the JSON node to convert.
     * @return the string representation, pretty printed.
     */
    public static String prettyPrint(JsonNode json) {
        return generateJson(json, true, false);
    }

    /**
     * Parses a String representing a json, and return it as a JsonNode.
     * @param src    the JSON string.
     * @return the JSON node.
     */
    public static JsonNode parse(String src) {
        try {
            return mapper().readTree(src);
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * New ObjectNode from pairs
     * <pre>{@code
     * JsonNode myJson = Json.obj(
     *         $("name", "Ragnar Lodbrock"),
     *         $("city", Some("Kattegat")),
     *         $("weight", 80),
     *         $("birthDate", LocalDate.of(766, 1, 1), $localdate()),
     *         $("sons", Json.arr(
     *             Json.obj("name", "Bjorn"),
     *             Json.obj("name", "Ubbe"),
     *             Json.obj("name", "Hvitserk"),
     *             Json.obj("name", "Sigurd"),
     *             Json.obj("name", "Ivar")
     *         ))
     * );
     * }</pre>
     * @param pairs array of field name / value pairs
     * @return the json object
     */
    public static ObjectNode obj(JsPair ...pairs) {
        return obj(newObject(), pairs);
    }

    public static ObjectNode obj(ObjectNode obj, JsPair ...pairs) {
        List.of(pairs).filter(p -> p.value.isDefined()).forEach(p -> {
            obj.set(p.field, p.value.get());
        });
        return obj;
    }

    public static ObjectNode merge(ObjectNode obj, ObjectNode obj2) {
        obj.fields().forEachRemaining(e ->
            obj2.set(e.getKey(), e.getValue())
        );
        return obj2;
    }

    /**
     * ArrayNode from JsonNodes
     * <pre>{@code
     * ArrayNode array = Json.arr(
     *     Json.obj("name", "Bjorn"),
     *     Json.obj("name", "Ubbe"),
     *     Json.obj("name", "Hvitserk"),
     *     Json.obj("name", "Sigurd"),
     *     Json.obj("name", "Ivar")
     * );
     * }</pre>
     * @param nodes array of json nodes
     * @return the json array
     */
    public static ArrayNode arr(JsonNode...nodes) {
        return arr(List.of(nodes));
    }

    /**
     * ArrayNode from JsonNodes
     * <pre>{@code
     * ArrayNode array = Json.arr(
     *     Json.obj("name", "Bjorn"),
     *     Json.obj("name", "Ubbe"),
     *     Json.obj("name", "Hvitserk"),
     *     Json.obj("name", "Sigurd"),
     *     Json.obj("name", "Ivar")
     * );
     * }</pre>
     * @param nodes array of json nodes
     * @return the json array
     */
    public static ArrayNode arr(Traversable<JsonNode> nodes) {
        ArrayNode array = newArray();
        nodes.forEach(array::add);
        return array;
    }

    /**
     * ArrayNode from strings
     * <pre>{@code
     * ArrayNode array = Json.arr(
     *     "Bjorn",
     *     "Ubbe",
     *     "Hvitserk",
     *     "Sigurd",
     *     "Ivar
     * );
     * }</pre>
     * @param nodes array of strings
     * @return the json array
     */
    public static ArrayNode arr(String ...nodes) {
        ArrayNode obj = newArray();
        List.of(nodes).map(TextNode::new).forEach(obj::add);
        return obj;
    }

    /**
     * An object field for int value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $(String field, Integer value) {
        return new JsPair(field, Option.of(value).map(IntNode::new));
    }
    /**
     * An object field for int value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $$(String field, Integer value) {
        return Json.$(field, value);
    }
    /**
     * An object field for long value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $(String field, Long value) {
        return new JsPair(field, Option.of(value).map(LongNode::new));
    }
    /**
     * An object field for long value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $$(String field, Long value) {
        return Json.$(field, value);
    }
    /**
     * An object field for a string option.
     * If the option is empty, the field is not written in the object.
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $(String field, Option<String> value) {
        return new JsPair(field, value.map(TextNode::new));
    }
    /**
     * An object field for a string option.
     * If the option is empty, the field is not written in the object.
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $$(String field, Option<String> value) {
        return Json.$(field, value);
    }
    /**
     * An object field for an enum value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $(String field, Enum<?> value) {
        return Json.$(field, Option.of(value).map(Enum::name));
    }
    /**
     * An object field for an enum value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $$(String field, Enum<?> value) {
        return Json.$(field, value);
    }
    /**
     * An object field for a string value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $(String field, String value) {
        return Json.$(field, Option.of(value));
    }
    /**
     * An object field for a string value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $$(String field, String value) {
        return Json.$(field, value);
    }
    /**
     * An object field for a boolean value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $(String field, Boolean value) {
        return new JsPair(field, Option.of(value).map(BooleanNode::valueOf));
    }
    /**
     * An object field for a boolean value
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $$(String field, Boolean value) {
        return Json.$(field, value);
    }
    /**
     * An object field for a nested json
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $(String field, JsonNode value) {
        return new JsPair(field, Option.of(value));
    }
    /**
     * An object field for a nested json
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static JsPair $$(String field, JsonNode value) {
        return Json.$(field, value);
    }
    /**
     * An object field for any value using a writer
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static <T> JsPair $(String field, T value, JsonWrite<T> jsonWrite) {
        return new JsPair(field, Option.of(value).map(jsonWrite::write));
    }
    /**
     * An object field for any value using a writer
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static <T> JsPair $$(String field, T value, JsonWrite<T> jsonWrite) {
        return Json.$(field, value, jsonWrite);
    }
    /**
     * An object field for any optional value using a writer
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static <T> JsPair $(String field, Option<T> value, JsonWrite<T> jsonWrite) {
        return new JsPair(field, value.map(jsonWrite::write));
    }
    /**
     * An object field for any optional value using a writer
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static <T> JsPair $$(String field, Option<T> value, JsonWrite<T> jsonWrite) {
        return Json.$(field, value, jsonWrite);
    }
    /**
     * An object field for any collection value using a writer
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static <T> JsPair $(String field, Traversable<T> value, JsonWrite<T> jsonWrite) {
        return new JsPair(field, Option.of(value).map(v -> arr(v.map(jsonWrite::write).toJavaArray(JsonNode[]::new))));
    }
    /**
     * An object field for any collection value using a writer
     * @param field the name
     * @param value the value
     * @return the field name / value pair
     */
    public static <T> JsPair $$(String field, Traversable<T> value, JsonWrite<T> jsonWrite) {
        return Json.$(field, value, jsonWrite);
    }

    public static class JsPair {
        final String field;
        final Option<JsonNode> value;

        public JsPair(String field, Option<JsonNode> value) {
            this.field = field;
            this.value = value;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", JsPair.class.getSimpleName() + "[", "]")
                    .add("field='" + field + "'")
                    .add("value=" + value)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsPair jsPair = (JsPair) o;
            return Objects.equals(field, jsPair.field) &&
                    Objects.equals(value, jsPair.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, value);
        }
    }
}
