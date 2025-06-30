package fr.maif.json;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.API;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Value;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.*;

import static io.vavr.API.Match;
import static io.vavr.API.None;
import static io.vavr.API.Some;
import static io.vavr.API.Tuple;

/**
 * A functional interface to describe a json to T reader.
 *
 * <pre>{@code
 * JsonRead<String> strRead = json -> {
 *    if (json.isTextual()) {
 *        return JsResult.success(json.asText());
 *    } else {
 *        return JsResult.error(List.of(JsResult.Error.error("string.expected")));
 *    }
 * };
 * }</pre>
 * @param <T>
 */
@FunctionalInterface
public interface JsonRead<T> {


    /**
     * Read a json and return a type T
     * @param jsonNode
     * @return the parsed object
     */
    JsResult<T> read(JsonNode jsonNode);

    /**
     *
     * @return the json schema for this read
     */
    default JsonSchema jsonSchema() {
        return JsonSchema.emptySchema();
    }

    /**
     * Create a read from a function and apply a schema.
     *
     * @param read
     * @param jsonSchema
     * @param <T>
     * @return
     */
    static <T> JsonRead<T> of (Function<JsonNode, JsResult<T>> read, JsonSchema jsonSchema) {
        return new JsonRead<T>() {
            @Override
            public JsResult<T> read(JsonNode jsonNode) {
                return read.apply(jsonNode);
            }

            @Override
            public JsonSchema jsonSchema() {
                return jsonSchema;
            }
        };
    }

    /**
     * Apply a schema to an existing read.
     * @param read
     * @param jsonSchema
     * @param <T>
     * @return
     */
    static <T> JsonRead<T> ofRead(JsonRead<T> read, JsonSchema jsonSchema) {
        return new JsonRead<T>() {
            @Override
            public JsResult<T> read(JsonNode jsonNode) {
                return read.read(jsonNode);
            }

            @Override
            public JsonSchema jsonSchema() {
                return jsonSchema;
            }
        };
    }

    /**
     * Combine two readers with a combine function.
     *
     * <pre>{@code
     * JsonRead<Tuple2<String, Integer>>> reader = _string("name").and(_int("age"), (String name, Integer age) ->  Tuple(name, age));
     * }</pre>
     *
     * @param other
     * @param func
     * @param <T2>
     * @param <R>
     * @return the reader
     */
    default <T2, R> JsonRead<R> and(JsonRead<T2> other, BiFunction<T, T2, R> func) {
        return JsonRead.of(json -> {
            JsResult<T> thisRead = read(json);
            JsResult<T2> otherRead = other.read(json);
            if (thisRead.isSuccess() && otherRead.isSuccess()) {
                return JsResult.success(func.apply(thisRead.get(), otherRead.get()));
            } else if (thisRead.isError() && otherRead.isError()) {
                return JsResult.error(thisRead.getErrors().appendAll(otherRead.getErrors()));
            } else if (thisRead.isError() && otherRead.isSuccess()) {
                return JsResult.error(thisRead.getErrors());
            } else {
                return JsResult.error(otherRead.getErrors());
            }
        }, this.jsonSchema().and(other.jsonSchema()));
    }

    /**
     * If this read doesn't succeed, switch to a fallback read.
     * @param fallback the fallback in case of failure.
     * @return the reader
     */
    default JsonRead<T> orElse(JsonRead<T> fallback) {
        return JsonRead.ofRead(jsonNode -> {
            JsResult<T> jsResult = read(jsonNode);
            if (jsResult.isError()) {
                return fallback.read(jsonNode);
            } else {
                return jsResult;
            }
        }, JsonSchema.oneOf(this.jsonSchema().computeSchema(), fallback.jsonSchema().computeSchema()));
    }

    /**
     * If this read doesn't succeed, switch to a fallback read.
     * @param value to use as default.
     * @return the reader
     */
    default JsonRead<T> orDefault(T value) {
        return orDefault(() -> value);
    }

    /**
     * If this read doesn't succeed, switch to a fallback read.
     * @param value to use as default.
     * @return the reader
     */
    default JsonRead<T> orDefault(Supplier<T> value) {
        return JsonRead.ofRead(jsonNode -> {
            JsResult<T> jsResult = read(jsonNode);
            if (jsResult.isError()) {
                return value(value).read(jsonNode);
            } else {
                return jsResult;
            }
        }, this.jsonSchema().computeSchema());
    }

    /**
     * Convert the json schema of this read
     * @param func the function to apply to schema
     * @return the updated reader
     */
    default JsonRead<T> mapSchema(Function<JsonSchema, JsonSchema> func) {
        return JsonRead.ofRead(
                this,
                func.apply(this.jsonSchema())
        );
    }

    /**
     * To specify the json schema $id
     * @param id
     * @return
     */
    default JsonRead<T> id(String id) {
        return mapSchema(s -> s.id(id));
    }

    /**
     * To specify the json schema $schema
     * @param schema
     * @return
     */
    default JsonRead<T> schema(String schema) {
        return mapSchema(s -> s.schema(schema));
    }

    /**
     * To specify the json schema title
     * @param title
     * @return
     */
    default JsonRead<T> title(String title) {
        return mapSchema(s -> s.title(title));
    }

    /**
     * To specify the json schema description
     * @param description
     * @return
     */
    default JsonRead<T> description(String description) {
        return mapSchema(s -> s.description(description));
    }

    /**
     * To specify the json schema exemples
     * @param exemples
     * @return
     */
    default JsonRead<T> exemples(List<JsonNode> exemples) {
        return mapSchema(s -> s.exemples(exemples));
    }

    /**
     * Reader from jackson introspection.
     * In this case, the json schema will be empty.
     * @param clazz
     * @param <T>
     * @return
     */
    static <T> JsonRead<T> _fromClass(Class<? extends T> clazz) {
        return JsonRead.ofRead(json -> Try
                .of(() -> Json.mapper().treeToValue(json, clazz))
                .fold(
                        e -> JsResult.error(JsResult.JsError.Error.error(e.getMessage())),
                        JsResult::success
                )
            , JsonSchema.emptySchema()
        );
    }

    /**
     * Reader from jackson introspection
     *
     * @param clazz
     * @param <T>
     * @return
     */
    static <T> JsonRead<T> _fromClass(TypeReference<T> clazz) {
        return JsonRead.ofRead(json -> Try
                .<T>of(() -> Json.mapper().convertValue(json, clazz))
                .fold(
                        e -> JsResult.error(JsResult.JsError.Error.error(e.getMessage())),
                        JsResult::success
                )
        , JsonSchema.emptySchema());
    }

    /**
     * Reader from constant value
     *
     * @param value
     * @param <T>
     * @return
     */
    static <T> JsonRead<T> value(T value) {
        return json -> JsResult.success(value);
    }

    /**
     * Reader from constant value
     *
     * @param value
     * @param <T>
     * @return
     */
    static <T> JsonRead<T> value(Supplier<T> value) {
        return json -> JsResult.success(value.get());
    }

    /**
     * Reader for ObjectNode
     *
     * <pre>{@code
     * JsonRead<ObjectNode> reader = __("objectNode", _jsonObject());
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<ObjectNode> _jsonObject() {
        return JsonRead.of(jsonNode -> {
          if (!Objects.isNull(jsonNode) && jsonNode.isObject()) {
              return JsResult.success((ObjectNode)jsonNode);
          } else {
              return JsResult.error(JsResult.Error.error("jsonobject.expected"));
          }
        }, JsonSchema.objectSchema());
    }

    /**
     * Reader for ArrayNode
     *
     * <pre>{@code
     * JsonRead<ArrayNode> reader = __("arrayNode", _jsonArray());
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<ArrayNode> _jsonArray() {
        return JsonRead.of(jsonNode -> {
          if (!Objects.isNull(jsonNode) && jsonNode.isArray()) {
              return JsResult.success((ArrayNode)jsonNode);
          } else {
              return JsResult.error(JsResult.Error.error("jsonarray.expected"));
          }
        }, JsonSchema.arraySchema());
    }

    static JsonRead<JsonNode> _json() {
        return JsResult::success;
    }

    /**
     * Read at path with any reader
     *
     * <pre>{@code
     * JsonRead<ArrayNode> reader = __("arrayNode", _jsonArray());
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param <T>
     * @return the reader
     */
    static <T> JsonRead<T> __(String path, JsonRead<T> r) {
        return JsonRead.of(json -> {
            if (Objects.isNull(json)) {
                return JsResult.error(JsResult.Error.error(path, "path.not.found"));
            }
            JsonNode jsonNode = json.get(path);
            if (json.has(path) && !jsonNode.isNull()) {
                return Try.of(() -> r.read(jsonNode))
                        .map(v -> v.mapError(errors -> errors.map(error -> error.repath(path))))
                        .getOrElseGet(e ->
                            JsResult.error(JsResult.Error.error(path, "error"))
                        );
            } else {
                return JsResult.error(JsResult.Error.error(path, "path.not.found"));
            }
        }, JsonSchema.propertySchema(path, r.jsonSchema()));
    }

    /**
     * Read at path with any reader and convert the result into a another type
     *
     * <pre>{@code
     * JsonRead<Integer> reader = __("node", _string(), str -> str.length());
     * }</pre>
     * @param path the field in json object
     * @param r the reader
     * @param func the convert function
     * @param <T>
     * @return the reader
     */
    static <T, R> JsonRead<R> __(String path, JsonRead<T> r, Function<T, R> func) {
        return __(path, r).map(func);
    }

    /**
     * Read an optional json at path with any reader
     *
     * <pre>{@code
     * JsonRead<Option<String>> reader = _opt("node", _string());
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param <T>
     * @return the reader
     */
    static <T> JsonRead<Option<T>> _opt(String path, JsonRead<? extends T> r) {
        return JsonRead.of(json -> {
            if (Objects.isNull(json) || !json.has(path) || json.get(path).isNull()) {
                return JsResult.success(Option.none());
            } else {
                return r.read(json.get(path))
                        .mapError(errors -> errors.map(error -> error.repath(path)))
                        .map(Option::of);
            }
        }, path == null ?
                r.jsonSchema() :
                JsonSchema.propertySchema(path, r.jsonSchema()).notRequired()
        );
    }

    /**
     * Read an optional json at path with any reader and convert the value
     *
     * <pre>{@code
     * JsonRead<Integer> reader = _opt("node", _string(), opt -> opt.fold(() -> 0, String::length));
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param func the convert function
     * @param <T>
     * @param <R>
     * @return the reader
     */
    static <T, R> JsonRead<R> _opt(String path, JsonRead<T> r, Function<Option<T>, R> func) {
        return _opt(path, r).map(func);
    }

    /**
     * Read a value at path that can be null
     *
     * <pre>{@code
     * JsonRead<Boolean> reader = _nullable("node", _string(), value -> value != null);
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param func the convert function
     * @param <T>
     * @param <R>
     * @return the reader
     */
    static <T, R> JsonRead<R> _nullable(String path, JsonRead<T> r, Function<T, R> func) {
        return JsonRead.ofRead(
                _opt(path, r).map(opt -> func.apply(opt.getOrNull())),
                JsonSchema.propertySchema(path, r.jsonSchema()).withRequired(false)
        );
    }

    /**
     * Read a value at path that can be null
     *
     * <pre>{@code
     * JsonRead<String> reader = _nullable("node", _string());
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param <T>
     * @return the reader
     */
    static <T> JsonRead<T> _nullable(String path, JsonRead<T> r) {
        return _opt(path, r).map(Value::getOrNull);
    }

    /**
     * Read a list at path with any reader
     *
     * <pre>{@code
     * JsonRead<List<String>> reader = __("node", _list(_string()));
     * }</pre>
     *
     * @param r the reader
     * @param <T>
     * @return the reader
     */
    static <T> JsonRead<List<T>> _list(JsonRead<? extends T> r) {
        return _list(null, r);
    }

    /**
     * Read a list at path with any reader
     *
     * <pre>{@code
     * JsonRead<List<String>> reader = _list("node", _string());
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param <T>
     * @return the reader
     */
    @SuppressWarnings("unchecked")
    static <T> JsonRead<List<T>> _list(String path, JsonRead<? extends T> r) {
        return JsonRead.of(jsonNode -> {
            Option<JsonNode> mayBeJson = Option.of(jsonNode);
            return mayBeJson.map(j -> {
                JsonNode json = Option.of(path).map(j::get).getOrElse(j);
                if (json == null || json.isNull()) {
                    return JsResult.success(List.<T>empty());
                }
                if (json.isArray()) {
                    List<JsResult<? extends T>> results = List.ofAll(json).filter(node -> node != null && !NullNode.getInstance().equals(node)).zipWithIndex().map(t ->
                            r.read(t._1).mapError(errors -> errors.map(error -> error.repath(Option.of(path).getOrElse("") + "["+t._2+"]")))
                    );
                    JsResult<Seq<T>> listJsResult = results.map(any -> (JsResult<T>)any).foldLeft(JsResult.success(List.empty()), (r1, r2) -> r2.combineMany(r1));
                    return listJsResult.map(Value::toList);
                } else {
                    return JsResult.<List<T>>error(JsResult.Error.error("array.expected"));
                }
            }).getOrElse(JsResult.success(List.empty()));
        }, path == null ?
                JsonSchema.arraySchema(r.jsonSchema()) :
                JsonSchema.propertySchema(path, JsonSchema.arraySchema(r.jsonSchema().computeSchema()))
        );
    }

    /**
     * Read a list at path with any reader and convert value
     *
     * <pre>{@code
     * JsonRead<Integer> reader = _list("node", _string(), l -> l.length());
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param func the convert function
     * @param <T>
     * @param <R>
     * @return the reader
     */
    @SuppressWarnings("unchecked")
    static <T, R> JsonRead<R> _list(String path, JsonRead<? extends T> r, Function<List<T>, R> func) {
        return _list(path, r).map(l -> func.apply((List<T>)l));
    }

    /**
     * Read a set at path with any reader and tranform value
     *
     * <pre>{@code
     * JsonRead<Integer> reader = _set("node", _string(), s -> s.length());
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param func the convert function
     * @param <T>
     * @param <R>
     * @return the reader
     */
    @SuppressWarnings("unchecked")
    static <T, R> JsonRead<R> _set(String path, JsonRead<? extends T> r, Function<Set<T>, R> func) {
        return _list(path, r).map(HashSet::ofAll).map(l -> func.apply((Set<T>)l));
    }

    /**
     * Read a set at path with any reader
     *
     * <pre>{@code
     * JsonRead<Set<String>> reader = _set("node", _string());
     * }</pre>
     *
     * @param path the field in json object
     * @param r the reader
     * @param <T>
     * @return the reader
     */
    static <T> JsonRead<Set<T>> _set(String path, JsonRead<? extends T> r) {
        return _list(path, r).map(HashSet::ofAll);
    }

    /**
     * oneOf is used to handle polymorphism. With oneOf you can use different reader based on the value of a discriminant type.
     *
     * For exemple if you have this json
     *
     * <pre>{@code
     *   {
     *       "type": "String",
     *       "value": "A text value in string"
     *   }
     * }</pre>
     * and this json
     * <pre>{@code
     *   {
     *       "type": "Integer",
     *       "value": 123456
     *   }
     * }</pre>
     *
     * You can deserialize with this code
     * <pre>{@code
     *
     * JsonRead<String> oneOfRead = JsonRead.oneOf(_string("type"), "value", List(
     *      caseOf("String"::equals, _string()),
     *      caseOf("Integer"::equals, _int().map(String::valueOf))
     * ));
     *
     * }</pre>
     *
     * The json schema generated will be a "oneOf" json schema
     *
     * @param argRead a reader to get the discriminant type
     * @param cases a list of cases to choose the right reader
     * @param <A>
     * @param <T>
     * @return the json read.
     */
    static <A, T> JsonRead<T> oneOf(JsonRead<A> argRead, List<ReadCase<? extends A, ? extends T>> cases) {
        return oneOf(argRead, None(), cases);
    }


    /**
     * oneOf is used to handle polymorphism. With oneOf you can use different reader based on the value of a discriminant type.
     *
     * For exemple if you have this json
     *
     * <pre>{@code
     *   {
     *       "type": "String",
     *       "value": "A text value in string"
     *   }
     * }</pre>
     * and this json
     * <pre>{@code
     *   {
     *       "type": "Integer",
     *       "value": 123456
     *   }
     * }</pre>
     *
     * You can deserialize with this code
     * <pre>{@code
     *
     * JsonRead<String> oneOfRead = JsonRead.oneOf(_string("type"), "value", List(
     *      caseOf("String"::equals, _string()),
     *      caseOf("Integer"::equals, _int().map(String::valueOf))
     * ));
     *
     * }</pre>
     *
     * The json schema generated will be a "oneOf" json schema
     *
     * @param argRead a reader to get the discriminant type
     * @param cases a list of cases to choose the right reader
     * @param <A>
     * @param <T>
     * @return the json read.
     */
    @SafeVarargs
    static <A, T> JsonRead<T> oneOf(JsonRead<A> argRead, ReadCase<? extends A, ? extends T>... cases) {
        return oneOf(argRead, None(), List.of(cases));
    }

    /**
     * oneOf is used to handle polymorphism. With oneOf you can use different reader based on the value of a discriminant type.
     *
     * For exemple if you have this json
     *
     * <pre>{@code
     *   {
     *       "type": "String",
     *       "value": "A text value in string"
     *   }
     * }</pre>
     * and this json
     * <pre>{@code
     *   {
     *       "type": "Integer",
     *       "value": 123456
     *   }
     * }</pre>
     *
     * You can deserialize with this code
     * <pre>{@code
     *
     * JsonRead<String> oneOfRead = JsonRead.oneOf(_string("type"), "value", List(
     *      caseOf("String"::equals, _string()),
     *      caseOf("Integer"::equals, _int().map(String::valueOf))
     * ));
     *
     * }</pre>
     *
     * The json schema generated will be a "oneOf" json schema
     *
     * @param argRead a reader to get the discriminant type
     * @param atPath a path to apply the readers from cases
     * @param cases a list of cases to choose the right reader
     * @param <A>
     * @param <T>
     * @return the json read.
     */
    static <A, T> JsonRead<T> oneOf(JsonRead<A> argRead, String atPath, List<ReadCase<? extends A, ? extends T>> cases) {
        return oneOf(argRead, Some(atPath), cases);
    }

    /**
     * oneOf is used to handle polymorphism. With oneOf you can use different reader based on the value of a discriminant type.
     *
     * For exemple if you have this json
     *
     * <pre>{@code
     *   {
     *       "type": "String",
     *       "value": "A text value in string"
     *   }
     * }</pre>
     * and this json
     * <pre>{@code
     *   {
     *       "type": "Integer",
     *       "value": 123456
     *   }
     * }</pre>
     *
     * You can deserialize with this code
     * <pre>{@code
     *
     * JsonRead<String> oneOfRead = JsonRead.oneOf(_string("type"), "value", List(
     *      caseOf("String"::equals, _string()),
     *      caseOf("Integer"::equals, _int().map(String::valueOf))
     * ));
     *
     * }</pre>
     *
     * The json schema generated will be a "oneOf" json schema
     *
     * @param argRead a reader to get the discriminant type
     * @param atPath a path to apply the readers from cases
     * @param cases a list of cases to choose the right reader
     * @param <A>
     * @param <T>
     * @return the json read.
     */
    @SafeVarargs
    static <A, T> JsonRead<T> oneOf(JsonRead<A> argRead, String atPath, ReadCase<? extends A, ? extends T>... cases) {
        return oneOf(argRead, Some(atPath), List.of(cases));
    }


    /**
     * oneOf is used to handle polymorphism. With oneOf you can use different reader based on the value of a discriminant type.
     *
     * For exemple if you have this json
     *
     * <pre>{@code
     *   {
     *       "type": "String",
     *       "value": "A text value in string"
     *   }
     * }</pre>
     * and this json
     * <pre>{@code
     *   {
     *       "type": "Integer",
     *       "value": 123456
     *   }
     * }</pre>
     *
     * You can deserialize with this code
     * <pre>{@code
     *
     * JsonRead<String> oneOfRead = JsonRead.oneOf(_string("type"), "value", List(
     *      caseOf("String"::equals, _string()),
     *      caseOf("Integer"::equals, _int().map(String::valueOf))
     * ));
     *
     * }</pre>
     *
     * The json schema generated will be a "oneOf" json schema
     *
     * @param argRead a reader to get the discriminant type
     * @param atPath a path to apply the readers from cases
     * @param cases a list of cases to choose the right reader
     * @param <A>
     * @param <T>
     * @return the json read.
     */
    @SuppressWarnings("unchecked")
    static <A, T> JsonRead<T> oneOf(JsonRead<A> argRead, Option<String> atPath, List<ReadCase<? extends A, ? extends T>> cases) {
        JsonSchema oneOfSchema = JsonSchema.oneOf(cases.map(ReadCase::jsonSchema));
        return JsonRead.ofRead(argRead.flatMap(r -> cases
                    .map(c -> (ReadCase<A, T>) c)
                    .flatMap(c -> atPath.map(p -> c.jsonRead(r).map(rr -> __(p, rr))).getOrElse(c.jsonRead(r)))
                    .headOption()
                    .getOrElse(() -> {
                        JsonRead<T> err = j -> JsResult.error(JsResult.Error.error("Not reader found for value", r));
                        return err;
                    })
                ),
                atPath.map(p -> (JsonSchema) JsonSchema.propertySchema(p, oneOfSchema)).getOrElse(oneOfSchema)
        );
    }

    /**
     * oneOf is used to handle polymorphism. With oneOf you can use different reader based on the value of a discriminant type.
     *
     * For exemple if you have this json
     *
     * <pre>{@code
     *   {
     *       "type": "dog",
     *       "version": "v1",
     *       "name": "Brutus"
     *   }
     * }</pre>
     *
     * this json
     *
     * <pre>{@code
     *  {
     *       "type": "dog",
     *       "version": "v2",
     *       "legacyName": "Brutus"
     *   }
     * }</pre>
     *
     * and this json
     *
     * <pre>{@code
     *  {
     *       "type": "cat",
     *       "version": "v1",
     *       "name": "Felix"
     *   }
     * }</pre>
     *
     * You can deserialize with this code
     * <pre>{@code
     *
     *    JsonRead<Animal> dogJsonReadV1 =
     *        _string("name", Dog.builder()::name)
     *        .map(Dog.DogBuilder::build);
     *
     *    JsonRead<Animal> dogJsonReadV2 =
     *        _string("legacyName", Dog.builder()::name)
     *        .map(Dog.DogBuilder::build);
     *
     *
     *    JsonRead<Animal> catJsonRead =
     *        _string("firstName", Cat.builder()::name)
     *        .map(Cat.CatBuilder::build);
     *
     *    JsonRead<Animal> oneOfRead = JsonRead.oneOf(_string("type"), _string("version"), List(
     *        caseOf((t, v) -> t.equals("dog") && v.equals("v1"), dogJsonReadV1),
     *        caseOf((t, v) -> t.equals("dog") && v.equals("v2"), dogJsonReadV2),
     *        caseOf((t, v) -> t.equals("cat") && v.equals("v1"), catJsonRead)
     *    ));
     *
     * }</pre>
     *
     * The json schema generated will be a "oneOf" json schema
     *
     * @param argRead1 a reader to get the first discriminant type
     * @param argRead2 a reader to get the first discriminant type
     * @param cases a list of cases to choose the right reader
     * @param <A>
     * @param <T>
     * @return the json read.
     */
    static <A, B, T> JsonRead<T> oneOf(JsonRead<A> argRead1, JsonRead<B> argRead2, List<ReadCase<? extends Tuple2<A, B>, ? extends T>> cases) {
        JsonRead<Tuple2<A, B>> and = argRead1.and(argRead2, Tuple::of);
        return JsonRead.<Tuple2<A, B>, T>oneOf(and, None(), cases);
    }

    /**
     * oneOf is used to handle polymorphism. With oneOf you can use different reader based on the value of a discriminant type.
     *
     * For exemple if you have this json
     *
     * <pre>{@code
     *   {
     *       "type": "dog",
     *       "version": "v1",
     *       "data": {
     *           "name": "Brutus"
     *       }
     *   }
     * }</pre>
     *
     * this json
     *
     * <pre>{@code
     *  {
     *       "type": "dog",
     *       "version": "v2",
     *       "data": {
     *           "legacyName": "Brutus"
     *       }
     *   }
     * }</pre>
     *
     * and this json
     *
     * <pre>{@code
     *  {
     *       "type": "cat",
     *       "version": "v1",
     *       "data": {
     *           "name": "Felix"
     *       }
     *   }
     * }</pre>
     *
     * You can deserialize with this code
     * <pre>{@code
     *
     *    JsonRead<Animal> dogJsonReadV1 =
     *        _string("name", Dog.builder()::name)
     *        .map(Dog.DogBuilder::build);
     *
     *    JsonRead<Animal> dogJsonReadV2 =
     *        _string("legacyName", Dog.builder()::name)
     *        .map(Dog.DogBuilder::build);
     *
     *
     *    JsonRead<Animal> catJsonRead =
     *        _string("firstName", Cat.builder()::name)
     *        .map(Cat.CatBuilder::build);
     *
     *    JsonRead<Animal> oneOfRead = JsonRead.oneOf(_string("type"), _string("version"), "data", List(
     *        caseOf((t, v) -> t.equals("dog") && v.equals("v1"), dogJsonReadV1),
     *        caseOf((t, v) -> t.equals("dog") && v.equals("v2"), dogJsonReadV2),
     *        caseOf((t, v) -> t.equals("cat") && v.equals("v1"), catJsonRead)
     *    ));
     *
     * }</pre>
     *
     * The json schema generated will be a "oneOf" json schema
     *
     * @param argRead1 a reader to get the first discriminant type
     * @param argRead2 a reader to get the first discriminant type
     * @param atPath a path to apply the readers from cases
     * @param cases a list of cases to choose the right reader
     * @param <A>
     * @param <T>
     * @return the json read.
     */
    static <A, B, T> JsonRead<T> oneOf(JsonRead<A> argRead1, JsonRead<B> argRead2, String atPath, List<ReadCase<? extends Tuple2<A, B>, ? extends T>> cases) {
        JsonRead<Tuple2<A, B>> and = argRead1.and(argRead2, Tuple::of);
        return JsonRead.oneOf(and, Option.of(atPath), cases);
    }

    /**
     * Build a match case for oneOf
     * @param match a predicate to match
     * @param read
     * @param <A>
     * @param <T>
     * @return
     */
    static <A,T> ReadCase<A, T> caseOf(Predicate<A> match, JsonRead<T> read) {
        return new ReadCase1<A, T>(match, read);
    }

    /**
     * Build a match case for oneOf
     * @param match a predicate to match
     * @param read
     * @param <A>
     * @param <T>
     * @return
     */
    static <A,T> ReadCase<A, T> caseOf(Match.Pattern0<? super A> match, JsonRead<T> read) {
        return new ReadCase1<A, T>(match::isDefinedAt, read);
    }

    /**
     * Build a match case for oneOf
     * @param match a predicate to match
     * @param read
     * @param <A>
     * @param <T>
     * @return
     */
    static <A, A1, T> ReadCase<A, T> caseOf(Match.Pattern1<A, A1> match, JsonRead<T> read) {
        return new ReadCase1<A, T>(match::isDefinedAt, read);
    }

    /**
     * Build a match case for oneOf
     * @param match
     * @param read
     * @param <A>
     * @param <B>
     * @param <T>
     * @return
     */
    static <A,B,T> ReadCase<Tuple2<A, B>, T> caseOf(BiPredicate<A, B> match, JsonRead<T> read) {
        return new ReadCase2<A, B, T>(match, read);
    }

    /**
     * Build a match case for oneOf
     * @param match
     * @param read
     * @param <A>
     * @param <B>
     * @param <T>
     * @return
     */
    static <A, A1, A2, B,T> ReadCase<Tuple2<A, B>, T> caseOf(API.Match.Pattern2<Tuple2<A, B>, A1, A2> match, JsonRead<T> read) {
        return new ReadCase2<A, B, T>((a, b) -> match.isDefinedAt(Tuple(a, b)), read);
    }

    interface ReadCase<A, T> {
        Option<JsonRead<T>> jsonRead(A value);
        JsonSchema jsonSchema();

        <T1> ReadCase<A, T1> map(Function<JsonRead<T>,  JsonRead<T1>> func);
    }

    class ReadCase1<A, T> implements ReadCase<A, T> {

        private final JsonRead<T> read;
        private final Predicate<A> match;

        public ReadCase1(Predicate<A> match, JsonRead<T> read) {
            this.read = read;
            this.match = match;
        }

        @Override
        public <T1> ReadCase<A, T1> map(Function<JsonRead<T>, JsonRead<T1>> func) {
            return new ReadCase1<>(match, func.apply(read));
        }

        @Override
        public Option<JsonRead<T>> jsonRead(A value) {
                if (match.test(value)) {
                    return Some(read);
                } else {
                    return None();
                }
        }

        @Override
        public JsonSchema jsonSchema() {
            return read.jsonSchema();
        }
    }

    class ReadCase2<A, B, T> implements ReadCase<Tuple2<A, B>, T> {

        private final JsonRead<T> read;
        private final BiPredicate<A, B> match;

        public ReadCase2(BiPredicate<A, B> match, JsonRead<T> read) {
            this.read = read;
            this.match = match;
        }

        @Override
        public <T1> ReadCase<Tuple2<A, B>, T1> map(Function<JsonRead<T>, JsonRead<T1>> func) {
            return new ReadCase2<>(match, func.apply(read));
        }

        @Override
        public Option<JsonRead<T>> jsonRead(Tuple2<A, B> value) {
            if (match.test(value._1, value._2)) {
                return Some(read);
            } else {
                return None();
            }
        }

        @Override
        public JsonSchema jsonSchema() {
            return read.jsonSchema();
        }
    }

    /**
     * Apply a function to the result.
     *
     * <pre>{@code
     * JsonRead<Integer> reader = _set("node", _string()).map(set -> set.length());
     * }</pre>
     *
     * @param func the convert function
     * @param <R>
     * @return the reader
     */
    default <R> JsonRead<R> map(Function<T, R> func) {
        return JsonRead.of(json -> read(json).map(func), this.jsonSchema());
    }

    /**
     * Compose reader
     *
     * @param func the convert function
     * @param <R>
     * @return the reader
     * @deprecated
     * flatMap is deprecated because the schema is lost when using it. Use oneOf instead.
     */
    @Deprecated
    default <R> JsonRead<R> flatMap(Function<T, JsonRead<R>> func) {
        return json -> read(json).flatMap(r -> func.apply(r).read(json));
    }

    /**
     * Compose reader
     *
     * @param func the convert function
     * @param <R>
     * @return the reader
     */
    default <R> JsonRead<R> flatMapResult(Function<T, JsResult<R>> func) {
        return json -> read(json).flatMap(func);
    }

    /**
     * A string reader
     *
     * @return the reader
     */
    static JsonRead<String> _string() {
        return JsonRead.of(json -> {
            if (!Objects.isNull(json) && json.isTextual()) {
                return JsResult.success(json.asText());
            } else {
                return JsResult.error(List.of(JsResult.Error.error("string.expected")));
            }
        }, JsonSchema.stringSchema());
    }

    /**
     * Read a string at path.
     *
     * <pre>{@code
     * JsonRead<String> reader = _string("node");
     * }</pre>
     *
     * @param path the field in json object
     * @return the reader
     */
    static JsonRead<String> _string(String path) {
        return __(path, _string());
    }

    /**
     * Read a string at path and convert value
     *
     * <pre>{@code
     * JsonRead<Integer> reader = _string("node", str -> str.length());
     * }</pre>
     *
     * @param path the field in json object
     * @param func the convert function
     * @param <R>
     * @return the reader
     */
    static <R> JsonRead<R> _string(String path, Function<String, R> func) {
        return __(path, _string()).map(func);
    }

    /**
     * Read a string and convert value
     *
     * <pre>{@code
     * JsonRead<Integer> reader = _string(str -> str.length());
     * }</pre>
     *
     * @param func the convert function
     * @param <R>
     * @return the reader
     */
    static <R> JsonRead<R> _string(Function<String, R> func) {
        return _string().map(func);
    }

    /**
     * Read a long value
     *
     * <pre>{@code
     * JsonRead<Long> reader = _long();
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<Long> _long() {
        return JsonRead.of(json -> JsResult.success(json.asLong()), JsonSchema.numberSchema());
    }

    /**
     * Read an int value
     *
     * <pre>{@code
     * JsonRead<Integer> reader = _int();
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<Integer> _int() {
        return JsonRead.of(json -> {
            if (!Objects.isNull(json) && json.isNumber()) {
                return JsResult.success(json.asInt());
            } else {
                return JsResult.error(JsResult.Error.error("number.expected"));
            }
        }, JsonSchema.integerSchema());
    }

    /**
     * Read an int value
     *
     * <pre>{@code
     * JsonRead<Integer> reader = _int("path");
     * }</pre>
     *
     * @param path the field in json object
     * @return the reader
     */
    static JsonRead<Integer> _int(String path) {
        return __(path, _int());
    }

    /**
     * Read an int value and transform value
     *
     * <pre>{@code
     * JsonRead<List<Integer>> reader = _int("path", intValue -> List.range(0, intValue));
     * }</pre>
     *
     * @param path the field in json object
     * @param func the convert function
     * @param <R>
     * @return the reader
     */
    static <R> JsonRead<R> _int(String path, Function<Integer, R> func) {
        return _int(path).map(func);
    }

    /**
     * Read an float value
     *
     * <pre>{@code
     * JsonRead<Float> reader = _float();
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<Float> _float() {
        return JsonRead.of(json -> {
            if (!Objects.isNull(json) && json.isNumber()) {
                return JsResult.success(json.numberValue().floatValue());
            } else {
                return JsResult.error(JsResult.Error.error("number.expected"));
            }
        }, JsonSchema.numberSchema());
    }

    /**
     * Read an float value
     *
     * <pre>{@code
     * JsonRead<Float> reader = _float("path");
     * }</pre>
     *
     * @param path the field in json object
     * @return the reader
     */
    static JsonRead<Float> _float(String path) {
        return __(path, _float());
    }

    /**
     * Read an float value and transform value
     *
     * <pre>{@code
     * JsonRead<List<Float>> reader = _float("path", value -> List.range(0, value));
     * }</pre>
     *
     * @param path the field in json object
     * @param func the convert function
     * @param <R>
     * @return the reader
     */
    static <R> JsonRead<R> _float(String path, Function<Float, R> func) {
        return _float(path).map(func);
    }
    /**
     * Read a double value
     *
     * <pre>{@code
     * JsonRead<Double> reader = _double();
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<Double> _double() {
        return JsonRead.of(json -> {
            if (!Objects.isNull(json) && json.isNumber()) {
                return JsResult.success(json.numberValue().doubleValue());
            } else {
                return JsResult.error(JsResult.Error.error("number.expected"));
            }
        }, JsonSchema.numberSchema());
    }

    /**
     * Read an double value
     *
     * <pre>{@code
     * JsonRead<Double> reader = _double("path");
     * }</pre>
     *
     * @param path the field in json object
     * @return the reader
     */
    static JsonRead<Double> _double(String path) {
        return __(path, _double());
    }

    /**
     * Read a double value and transform value
     *
     * <pre>{@code
     * JsonRead<List<Double>> reader = _double("path", value -> List.range(0, value));
     * }</pre>
     *
     * @param path the field in json object
     * @param func the convert function
     * @param <R>
     * @return the reader
     */
    static <R> JsonRead<R> _double(String path, Function<Double, R> func) {
        return _double(path).map(func);
    }

    /**
     * Read an big decimal value
     *
     * <pre>{@code
     * JsonRead<BigDecimal> reader = _bigDecimal();
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<BigDecimal> _bigDecimal() {
        return JsonRead.of(json -> {
            if (!Objects.isNull(json) && json.isTextual()) {
                return Try.of(() ->
                        JsResult.success(new BigDecimal(json.asText()).setScale(2, RoundingMode.HALF_UP))
                ).getOrElseGet(e ->
                        JsResult.error(JsResult.Error.error("pattern.invalid"))
                );
            } else {
                return JsResult.error(JsResult.Error.error("string.expected"));
            }
        }, JsonSchema.numberSchema());
    }

    /**
     * Read an optional string at path
     *
     * <pre>{@code
     * JsonRead<Option<String>> reader = _optString("node");
     * }</pre>
     *
     * @param path the field in json object
     * @return the reader
     */
    static JsonRead<Option<String>> _optString(String path) {
        return _opt(path, _string());
    }

    /**
     * Read a date at path
     *
     * <pre>{@code
     * JsonRead<LocalDate> reader = _localDate(DateTimeFormatter.ISO_DATE);
     * }</pre>
     *
     * @param formatter
     * @return the reader
     */
    static JsonRead<LocalDate> _localDate(DateTimeFormatter formatter) {
        return JsonRead.ofRead(_string().flatMapResult(str ->
            Try.of(() -> LocalDate.parse(str, formatter))
                    .map(JsResult::success)
                    .getOrElseGet(e -> JsResult.error(JsResult.Error.error("pattern.invalid")))
        ), JsonSchema.dateSchema());
    }

    /**
     * Read a date at path with iso format
     *
     * <pre>{@code
     * JsonRead<LocalDate> reader = _isoLocalDate();
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<LocalDate> _isoLocalDate() {
            return _localDate(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Read a datetime at path with iso format
     *
     * <pre>{@code
     * JsonRead<LocalDate> reader = _isoLocalDate();
     * }</pre>
     *
     * @return the reader
     */
    static JsonRead<Instant> _instant() {
        return JsonRead.ofRead(_string().flatMapResult(str ->
                Try.of(() -> Instant.parse(str))
                        .map(JsResult::success)
                        .getOrElseGet(e -> JsResult.error(JsResult.Error.error("cannot.parse.into.instant")))
        ), JsonSchema.dateTimeSchema());
    }

    /**
     * Read a date at path
     *
     * <pre>{@code
     * JsonRead<LocalDateTime> reader = _localDate(DateTimeFormatter.ISO_DATE);
     * }</pre>
     *
     * @param formatter
     * @return the reader
     */
    static JsonRead<LocalDateTime> _localDateTime(DateTimeFormatter formatter) {
        return JsonRead.ofRead(_string().flatMapResult(str ->
                Try.of(() -> LocalDateTime.parse(str, formatter))
                        .map(JsResult::success)
                        .getOrElseGet(e -> JsResult.error(JsResult.Error.error("pattern.invalid")))
        ), JsonSchema.dateTimeSchema());
    }

    /**
     * Read a date at path with iso format.
     *
     * <pre>{@code
     * JsonRead<LocalDateTime> reader = _isoLocalDateTime();
     * }</pre>
     * @return the reader
     */
    static JsonRead<LocalDateTime> _isoLocalDateTime() {
            return _localDateTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Read a boolean
     *
     * <pre>{@code
     * JsonRead<Boolean> reader = _boolean();
     * }</pre>
     *
     * @return the reader
     * @return the reader
     */
    static JsonRead<Boolean> _boolean() {
        return JsonRead.of(json -> {
            if (!Objects.isNull(json) && json.isBoolean()) {
                return JsResult.success(json.asBoolean());
            } else {
                return JsResult.error(JsResult.Error.error("boolean.expected"));
            }
        }, JsonSchema.booleanSchema());
    }

    /**
     * Read a boolean at path
     *
     * <pre>{@code
     * JsonRead<Boolean> reader = _boolean("path");
     * }</pre>
     *
     * @param path the field in json object
     * @return the reader
     */
    static JsonRead<Boolean> _boolean(String path) {
        return __(path, _boolean());
    }

    /**
     * Read an enum
     *
     * <pre>{@code
     * JsonRead<MyEnum> reader = _enum(MyEnum.class);
     * }</pre>
     *
     * @param clazz
     * @param <E>
     * @return the reader
     */
    static <E extends Enum<E>> JsonRead<E> _enum(Class<E> clazz) {
        return JsonRead.ofRead(_string().flatMapResult(str ->
            Try.of(() -> E.valueOf(clazz, str))
                    .map(JsResult::success)
                    .getOrElseGet(e ->
                        JsResult.error(JsResult.Error.error("invalid.enum.value", (Object[]) clazz.getEnumConstants()))
                    )
        ), JsonSchema.enumSchema(clazz));
    }

    /**
     * Read an enum at path
     *
     * <pre>{@code
     * JsonRead<MyEnum> reader = _enum("path", MyEnum.class);
     * }</pre>
     *
     * @param path the field in json object
     * @param clazz
     * @param <E>
     * @return the reader
     */
    static <E extends Enum<E>> JsonRead<E> _enum(String path, Class<E> clazz) {
        JsonRead<E> enumread = _enum(clazz);
        return JsonRead.ofRead(__(path, enumread), JsonSchema.propertySchema(path, enumread.jsonSchema()));
    }

}
