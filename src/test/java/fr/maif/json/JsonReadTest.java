package fr.maif.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.vavr.API;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lombok.*;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import static fr.maif.json.Json.$$;
import static fr.maif.json.JsonRead.*;
import static io.vavr.API.List;
import static io.vavr.API.println;
import static io.vavr.Patterns.$Some;
import static io.vavr.Patterns.$Tuple2;
import static fr.maif.json.Json.$;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonReadTest {

    JsonSchema jsonSchemaObject = pojoJsonReadV1
            .schema("http://json-schema.org/draft-07/schema")
            .title("The root schema")
            .id("http://example.com/example.json")
            .description("The root schema comprises the entire JSON document.")
            .exemples(List(json))
            .jsonSchema();

    static JsonRead<Pojo2> pojo2Read = _string("value", Pojo2.builder()::value).map(Pojo2.Pojo2Builder::build);

    static JsonRead<Pojo> pojoJsonReadV1 =
            _string("aString", Pojo.builder()::aString)
                    .and(_optString("optString"), Pojo.PojoBuilder::optString)
                    .and(_list("pojos", pojo2Read), Pojo.PojoBuilder::pojos)
                    .and(_opt("optLocalDate", _isoLocalDate()), Pojo.PojoBuilder::optLocalDate)
                    .map(Pojo.PojoBuilder::build);


    static LocalDate aDate = LocalDate.now();

    static ObjectNode json = Json.obj(
            $("aString", "A string"),
            $("pojos", Json.arr(Json.obj($("value", "A value")))),
            $("optLocalDate", DateTimeFormatter.ISO_LOCAL_DATE.format(aDate))
    );

    static JsonNode expectedJsonSchema = Json.obj(
            $$("type", "object"),
            $$("required", Json.arr("aString", "pojos")),
            $$("properties", Json.obj(
                    $$("optLocalDate", Json.obj(
                            $$("type", "string"),
                            $$("format", "date")
                    )),
                    $$("pojos", Json.obj(
                            $$("type", "array"),
                            $$("items", Json.obj(
                                    $$("type", "object"),
                                    $$("required", Json.arr("value")),
                                    $$("properties", Json.obj(
                                            $$("value", Json.obj(
                                                    $$("type", "string")
                                            ))
                                    ))
                            ))
                    )),
                    $$("optString", Json.obj(
                            $$("type", "string")
                    )),
                    $$("aString", Json.obj(
                            $$("type", "string")
                    ))
            )),
            $$("$schema", "http://json-schema.org/draft-07/schema"),
            $$("$id", "http://example.com/example.json"),
            $$("title", "The root schema"),
            $$("description", "The root schema comprises the entire JSON document."),
            $$("exemples", Json.arr(json))
    );

    @Test
    public void stringValid() {
        JsResult<String> test = _string().read(new TextNode("test"));
        assertThat(test).isEqualTo(JsResult.success("test"));
        assertThat(_string().jsonSchema()).isEqualTo(JsonSchema.stringSchema());
    }

    @Test
    public void stringValidTransform() {
        JsResult<String> test = _string(String::toUpperCase).read(new TextNode("test"));
        assertThat(test).isEqualTo(JsResult.success("TEST"));
        assertThat(_string().jsonSchema()).isEqualTo(JsonSchema.stringSchema());
    }

    @Test
    public void stringInvalid() {
        JsResult<String> test = _string().read(new IntNode(0));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("string.expected")));
    }

    @Test
    public void longValid() {
        JsResult<Long> test = _long().read(new IntNode(0));
        assertThat(test).isEqualTo(JsResult.success(0L));
        assertThat(_int().jsonSchema()).isEqualTo(JsonSchema.integerSchema());
    }

    @Test
    public void intValid() {
        JsResult<Integer> test = _int().read(new IntNode(0));
        assertThat(test).isEqualTo(JsResult.success(0));
        assertThat(_int().jsonSchema()).isEqualTo(JsonSchema.integerSchema());
    }

    @Test
    public void intValidAtPath() {
        JsonRead<Integer> integerJsonRead = _int("field", i -> i + 1);
        JsResult<Integer> test = integerJsonRead.read(Json.obj($$("field", 0)));
        assertThat(test).isEqualTo(JsResult.success(1));
        assertThat(integerJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("field", JsonSchema.integerSchema()));
    }

    @Test
    public void intInvalid() {
        JsResult<Integer> test = _int().read(new TextNode("test"));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("number.expected")));
    }

    @Test
    public void optValid() {
        JsonRead<Option<Integer>> read = _opt("path", _int());
        JsResult<Option<Integer>> test = read.read(Json.obj($("path", 0)));
        assertThat(test).isEqualTo(JsResult.success(Option.of(0)));
        assertThat(read.jsonSchema()).isEqualTo(JsonSchema.propertySchema("path", JsonSchema.integerSchema()).notRequired());
    }

    @Test
    public void optValidTransform() {
        JsonRead<Option<Integer>> read = _opt("path", _int(), mayBeI -> mayBeI.map(i -> i + 1));
        JsResult<Option<Integer>> test = read.read(Json.obj($("path", 0)));
        assertThat(test).isEqualTo(JsResult.success(Option.of(1)));
        assertThat(read.jsonSchema()).isEqualTo(JsonSchema.propertySchema("path", JsonSchema.integerSchema()).notRequired());
    }

    @Test
    public void optInvalid() {
        JsResult<Option<Integer>> test = _opt("path", _int()).read(Json.obj());
        assertThat(test).isEqualTo(JsResult.success(Option.none()));
    }

    @Test
    public void dateValid() {
        LocalDate date = LocalDate.now();
        JsonRead<LocalDate> localDateJsonRead = _localDate(DateTimeFormatter.ISO_LOCAL_DATE);
        JsResult<LocalDate> test = localDateJsonRead.read(new TextNode(DateTimeFormatter.ISO_LOCAL_DATE.format(date)));
        assertThat(test).isEqualTo(JsResult.success(date));
        assertThat(localDateJsonRead.jsonSchema()).isEqualTo(JsonSchema.dateSchema());
    }

    @Test
    public void dateInvalid() {
        JsResult<LocalDate> test = _localDate(DateTimeFormatter.ISO_LOCAL_DATE).read(new TextNode("test"));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("pattern.invalid")));
    }


    @Test
    public void dateTimeValid() {
        LocalDateTime date = LocalDateTime.now();
        JsonRead<LocalDateTime> localDateJsonRead = _localDateTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        JsResult<LocalDateTime> test = localDateJsonRead.read(new TextNode(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date)));
        assertThat(test).isEqualTo(JsResult.success(date));
        assertThat(localDateJsonRead.jsonSchema()).isEqualTo(JsonSchema.dateTimeSchema());
    }

    @Test
    public void isoLocalDateTimeValid() {
        LocalDateTime date = LocalDateTime.now();
        JsonRead<LocalDateTime> localDateJsonRead = _isoLocalDateTime();
        JsResult<LocalDateTime> test = localDateJsonRead.read(new TextNode(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date)));
        assertThat(test).isEqualTo(JsResult.success(date));
        assertThat(localDateJsonRead.jsonSchema()).isEqualTo(JsonSchema.dateTimeSchema());
    }

    @Test
    public void dateTimeInvalid() {
        JsResult<LocalDateTime> test = _localDateTime(DateTimeFormatter.ISO_LOCAL_DATE).read(new TextNode("test"));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("pattern.invalid")));
    }

    @Test
    public void booleanValid() {
        JsResult<Boolean> test = _boolean().read(BooleanNode.TRUE);
        assertThat(test).isEqualTo(JsResult.success(true));
        assertThat(_boolean().jsonSchema()).isEqualTo(JsonSchema.booleanSchema());
    }

    @Test
    public void booleanValidAtPath() {
        JsonRead<Boolean> booleanJsonRead = _boolean("field");
        JsResult<Boolean> test = booleanJsonRead.read(Json.obj($$("field", true)));
        assertThat(test).isEqualTo(JsResult.success(true));
        assertThat(booleanJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("field", JsonSchema.booleanSchema()));
    }

    @Test
    public void booleanInvalid() {
        JsResult<Boolean> test = _boolean().read(new TextNode("test"));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("boolean.expected")));
    }

    @Test
    public void pathValid() {
        JsonRead<Integer> pathRead = __("path", _int());
        JsResult<Integer> test = pathRead.read(Json.obj($("path", 0)));
        assertThat(test).isEqualTo(JsResult.success(0));
        assertThat(pathRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("path", JsonSchema.integerSchema()));
    }


    @Test
    public void pathTransformValid() {
        JsonRead<Integer> pathRead = __("path", _int(), i -> i + 1);
        JsResult<Integer> test = pathRead.read(Json.obj($("path", 0)));
        assertThat(test).isEqualTo(JsResult.success(1));
        assertThat(pathRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("path", JsonSchema.integerSchema()));
    }

    @Test
    public void pathInvalid() {
        JsResult<Integer> test = __("path", _int()).read(Json.obj());
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("path", "path.not.found")));
    }

    @Test
    public void pathInvalidExceptionThrown() {
        JsonRead<Integer> intRead = (value) -> { throw new RuntimeException("Ouops");};
        JsResult<Integer> test = __("path", intRead).read(Json.obj($("path", 1)));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("path", "error")));
    }

    @Test
    public void pathValueInvalid() {
        JsResult<Integer> test = __("path", _int()).read(Json.obj($("path", "Test")));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("path", "number.expected")));
    }

    @Test
    public void setValid() {
        JsonRead<Set<String>> listJsonRead = _set("field", _string());
        JsResult<Set<String>> test = listJsonRead.read(Json.obj($$("field", Json.arr("value1", "value1", "value2"))));
        assertThat(test).isEqualTo(JsResult.success(HashSet.of("value1", "value2")));
        assertThat(listJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("field", JsonSchema.arraySchema(JsonSchema.stringSchema())));
    }

    @Test
    public void setValidTransform() {
        JsonRead<Set<String>> listJsonRead = _set("field", _string(), s -> s.map(String::toUpperCase));
        JsResult<Set<String>> test = listJsonRead.read(Json.obj($$("field", Json.arr("value1", "value1", "value2"))));
        assertThat(test).isEqualTo(JsResult.success(HashSet.of("VALUE1", "VALUE2")));
        assertThat(listJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("field", JsonSchema.arraySchema(JsonSchema.stringSchema())));
    }

    @Test
    public void listValid() {
        JsonRead<List<String>> listJsonRead = _list(_string());
        JsResult<List<String>> test = listJsonRead.read(Json.arr("value1", "value2"));
        assertThat(test).isEqualTo(JsResult.success(List.of("value1", "value2")));
        assertThat(listJsonRead.jsonSchema()).isEqualTo(JsonSchema.arraySchema(JsonSchema.stringSchema()));
    }

    @Test
    public void listTransformValid() {
        JsonRead<List<String>> listJsonRead = _list("field", _string(), s -> s.map(String::toUpperCase));
        JsResult<List<String>> test = listJsonRead.read(Json.obj($$("field", Json.arr("value1", "value2"))));
        assertThat(test).isEqualTo(JsResult.success(List.of("VALUE1", "VALUE2")));
        assertThat(listJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("field", JsonSchema.arraySchema(JsonSchema.stringSchema())));
    }

    @Test
    public void listInvalid() {
        JsResult<List<String>> test = _list(_string()).read(Json.obj());
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("array.expected")));
    }


    @Test
    public void listValueInvalid() {
        JsResult<List<String>> test = _list(_string()).read(Json.newArray().add("value1").add(0));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("[1]", "string.expected")));
    }


    @Test
    public void listValueValidAtPath() {
        JsonRead<List<String>> listJsonRead = _list("path", _string());
        JsResult<List<String>> test = listJsonRead.read(Json.obj($("path", Json.arr("value1"))));
        assertThat(test).isEqualTo(JsResult.success(List.of("value1")));
        assertThat(listJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("path", JsonSchema.arraySchema(JsonSchema.stringSchema())));
    }

    @Test
    public void listValueInvalidAtPath() {
        JsResult<List<String>> test = _list("path", _string()).read(Json.obj($("path", Json.newArray().add("value1").add(0))));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("path[1]", "string.expected")));
    }

    @Test
    public void jsonValid() {
        JsResult<JsonNode> test = _json().read(Json.obj());
        assertThat(test).isEqualTo(JsResult.success(Json.obj()));
        assertThat(_json().jsonSchema()).isEqualTo(JsonSchema.emptySchema());
    }


    @Test
    public void jsonArrayValid() {
        JsResult<ArrayNode> test = _jsonArray().read(Json.arr("test"));
        assertThat(test).isEqualTo(JsResult.success(Json.arr("test")));
        assertThat(_jsonArray().jsonSchema()).isEqualTo(JsonSchema.arraySchema());
    }

    @Test
    public void jsonArrayInvalid() {
        JsResult<ArrayNode> test = _jsonArray().read(Json.obj());
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("jsonarray.expected")));
    }

    @Test
    public void jsonObjectValid() {
        JsResult<ObjectNode> test = _jsonObject().read(Json.obj());
        assertThat(test).isEqualTo(JsResult.success(Json.obj()));
        assertThat(_jsonObject().jsonSchema()).isEqualTo(JsonSchema.objectSchema());
    }

    @Test
    public void jsonObjectInvalid() {
        JsResult<ObjectNode> test = _jsonObject().read(Json.arr("test"));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("jsonobject.expected")));
    }

    @Test
    public void enumValid() {
        JsonRead<TestEnum> enumJsonRead = _enum(TestEnum.class);
        JsResult<TestEnum> test = enumJsonRead.read(new TextNode("test1"));
        assertThat(test).isEqualTo(JsResult.success(TestEnum.test1));
        assertThat(enumJsonRead.jsonSchema()).isEqualTo(JsonSchema.enumSchema(TestEnum.class));
    }

    @Test
    public void enumValidAtPath() {
        JsonRead<TestEnum> enumJsonRead = _enum("field", TestEnum.class);
        JsResult<TestEnum> test = enumJsonRead.read(Json.obj($$("field", "test1")));
        assertThat(test).isEqualTo(JsResult.success(TestEnum.test1));
        assertThat(enumJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("field", JsonSchema.enumSchema(TestEnum.class)));
    }

    @Test
    public void enumInvalid() {
        JsResult<TestEnum> test = _enum(TestEnum.class).read(new TextNode("test"));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("invalid.enum.value", (Object[]) TestEnum.values())));
    }

    @Test
    public void bigDecimalValid() {
        JsResult<BigDecimal> test = _bigDecimal().read(new TextNode("50.00"));
        assertThat(test).isEqualTo(JsResult.success(BigDecimal.valueOf(5000, 2)));
        assertThat(_bigDecimal().jsonSchema()).isEqualTo(JsonSchema.numberSchema());
    }

    @Test
    public void bigDecimalPatternInvalid() {
        JsResult<BigDecimal> test = _bigDecimal().read(new TextNode("toto"));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("pattern.invalid")));
    }

    @Test
    public void bigDecimalJsonNodeInvalid() {
        JsResult<BigDecimal> test = _bigDecimal().read(new IntNode(0));
        assertThat(test).isEqualTo(JsResult.error(JsResult.Error.error("string.expected")));
    }

    @Test
    public void readOrElse() {
        JsonRead<String> stringJsonRead = _string("path").orElse(_list("path", _string()).map(l -> l.mkString(",")));
        JsResult<String> jsResult = stringJsonRead.read(Json.obj($("path", Json.arr("1", "2", "3"))));
        assertThat(jsResult).isEqualTo(JsResult.success("1,2,3"));
        assertThat(stringJsonRead.jsonSchema()).isEqualTo(JsonSchema.oneOf(
                JsonSchema.objectSchema("path", JsonSchema.stringSchema()),
                JsonSchema.objectSchema("path", JsonSchema.arraySchema(JsonSchema.stringSchema()))
        ));
    }

    @Test
    public void nullableRead() {
        JsonRead<String> stringJsonRead = _nullable("test", _string());
        String s = stringJsonRead.read(Json.obj()).get();
        assertThat(s).isNull();
        String s2 = stringJsonRead.read(Json.obj($$("test", "value"))).get();
        assertThat(s2).isEqualTo("value");
        assertThat(stringJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("test", JsonSchema.stringSchema()).notRequired());
    }

    @Test
    public void nullableTransformRead() {
        JsonRead<String> stringJsonRead = _nullable("test", _string(), s -> Option.of(s).map(String::toUpperCase).getOrNull());
        String s = stringJsonRead.read(Json.obj()).get();
        assertThat(s).isNull();
        String s2 = stringJsonRead.read(Json.obj($$("test", "value"))).get();
        assertThat(s2).isEqualTo("VALUE");
        assertThat(stringJsonRead.jsonSchema()).isEqualTo(JsonSchema.propertySchema("test", JsonSchema.stringSchema()).notRequired());
    }

    @Test
    public void flatMap() {
        JsonRead<Pojo> newPojoRead = _string("aString").flatMap(s -> pojoJsonReadV1);

        JsResult<Pojo> read = newPojoRead.read(json);

        assertThat(read).isEqualTo(JsResult.success(Pojo.builder()
                .aString("A string")
                .optLocalDate(Option.none())
                .pojos(List.of(new Pojo2("A value", null)))
                .optLocalDate(Option.of(aDate))
                .build()
        ));
    }

    @Test
    public void oneOfTest() {
        JsonRead<String> oneOfRead = JsonRead.oneOf(_string("type"), "value", List(
                caseOf("type1"::equals, _string()),
                caseOf("type2"::equals, _int().map(String::valueOf))
        ));

        String value1 = oneOfRead.read(Json.obj(
                $$("type", "type1"),
                $$("value", "string value")
        )).get();


        String value2 = oneOfRead.read(Json.obj(
                $$("type", "type2"),
                $$("value", 2)
        )).get();

        assertThat(value1).isEqualTo("string value");
        assertThat(value2).isEqualTo("2");

        assertThat(oneOfRead.jsonSchema()).isEqualTo(
                JsonSchema.propertySchema("value", JsonSchema.oneOf(List(
                        JsonSchema.stringSchema(),
                        JsonSchema.integerSchema()
                )))
        );
    }

    @Test
    public void oneOfMultipleDiscriminantTest() {

        JsonRead<Animal> dogJsonReadV1 =
                _string("name", Dog.builder()::name)
                    .map(Dog.DogBuilder::build);

        JsonRead<Animal> dogJsonReadV2 =
                _string("legacyName", Dog.builder()::name)
                    .map(Dog.DogBuilder::build);


        JsonRead<Animal> catJsonRead =
                _string("name", Cat.builder()::name)
                    .map(Cat.CatBuilder::build);

        JsonRead<Animal> oneOfRead = JsonRead.oneOf(_string("type"), _string("version"), "data", List(
                caseOf((t, v) -> t.equals("dog") && v.equals("v1"), dogJsonReadV1),
                caseOf((t, v) -> t.equals("dog") && v.equals("v2"), dogJsonReadV2),
                caseOf((t, v) -> t.equals("cat") && v.equals("v1"), catJsonRead)
        ));

        Animal value1 = oneOfRead.read(Json.obj(
                $$("type", "dog"),
                $$("version", "v1"),
                $$("data", Json.obj(
                        $$("name", "Brutus")
                ))
        )).get();

        Animal value2 = oneOfRead.read(Json.obj(
                $$("type", "dog"),
                $$("version", "v2"),
                $$("data", Json.obj(
                        $$("legacyName", "Brutus")
                ))
        )).get();

        Animal value3 = oneOfRead.read(Json.obj(
                $$("type", "cat"),
                $$("version", "v1"),
                $$("data", Json.obj(
                        $$("name", "Felix")
                ))
        )).get();

        assertThat(value1).isEqualTo(new Dog("Brutus"));
        assertThat(value2).isEqualTo(new Dog("Brutus"));
        assertThat(value3).isEqualTo(new Cat("Felix"));

        assertThat(oneOfRead.jsonSchema()).isEqualTo(
                JsonSchema.propertySchema("data", JsonSchema.oneOf(List(
                        dogJsonReadV1.jsonSchema(),
                        dogJsonReadV2.jsonSchema(),
                        catJsonRead.jsonSchema()
                )))
        );
    }

    @Test
    public void oneOfMultipleDiscriminantAtRootTest() {

        JsonRead<Animal> dogJsonReadV1 =
                _string("name", Dog.builder()::name)
                    .map(Dog.DogBuilder::build);

        JsonRead<Animal> dogJsonReadV2 =
                _string("legacyName", Dog.builder()::name)
                    .map(Dog.DogBuilder::build);


        JsonRead<Animal> catJsonRead =
                _string("name", Cat.builder()::name)
                    .map(Cat.CatBuilder::build);

        JsonRead<Animal> oneOfRead = JsonRead.oneOf(_string("type"), _string("version"), List(
                caseOf($Tuple2(API.$("dog"), API.$("v1")), dogJsonReadV1),
                caseOf($Tuple2(API.$("dog"), API.$("v2")), dogJsonReadV2),
                caseOf($Tuple2(API.$("cat"), API.$("v1")), catJsonRead)
        ));

        Animal value1 = oneOfRead.read(Json.obj(
                $$("type", "dog"),
                $$("version", "v1"),
                $$("name", "Brutus")
        )).get();

        Animal value2 = oneOfRead.read(Json.obj(
                $$("type", "dog"),
                $$("version", "v2"),
                $$("legacyName", "Brutus")
        )).get();

        Animal value3 = oneOfRead.read(Json.obj(
                $$("type", "cat"),
                $$("version", "v1"),
                $$("name", "Felix")
        )).get();

        assertThat(value1).isEqualTo(new Dog("Brutus"));
        assertThat(value2).isEqualTo(new Dog("Brutus"));
        assertThat(value3).isEqualTo(new Cat("Felix"));

        assertThat(oneOfRead.jsonSchema()).isEqualTo(
                JsonSchema.oneOf(List(
                        dogJsonReadV1.jsonSchema(),
                        dogJsonReadV2.jsonSchema(),
                        catJsonRead.jsonSchema()
                ))
        );
    }

    enum AnimalType {
        dog, cat;
    }

    @Test
    public void oneOfWithVavrPattern0() {

        JsonRead<Animal> dogJsonReadV1 =
                _string("name", Dog.builder()::name)
                    .map(Dog.DogBuilder::build);

        JsonRead<Animal> catJsonRead =
                _string("name", Cat.builder()::name)
                    .map(Cat.CatBuilder::build);

        JsonRead<Animal> oneOfRead = JsonRead.oneOf(_enum("type", AnimalType.class), List.of(
                caseOf(API.$(AnimalType.dog), dogJsonReadV1),
                caseOf(API.$(AnimalType.cat), catJsonRead),
                caseOf(API.$(), catJsonRead)
        ));

        Animal value1 = oneOfRead.read(Json.obj(
                $$("type", "dog"),
                $$("version", "v1"),
                $$("name", "Brutus")
        )).get();

        Animal value3 = oneOfRead.read(Json.obj(
                $$("type", "cat"),
                $$("version", "v1"),
                $$("name", "Felix")
        )).get();

        assertThat(value1).isEqualTo(new Dog("Brutus"));
        assertThat(value3).isEqualTo(new Cat("Felix"));

        assertThat(oneOfRead.jsonSchema()).isEqualTo(
                JsonSchema.oneOf(List(
                        dogJsonReadV1.jsonSchema(),
                        catJsonRead.jsonSchema()
                ))
        );
    }
    @Test
    public void oneOfWithVavrPattern1() {

        JsonRead<Animal> dogJsonReadV1 =
                _string("name", Dog.builder()::name)
                    .map(Dog.DogBuilder::build);

        JsonRead<Animal> catJsonRead =
                _string("name", Cat.builder()::name)
                    .map(Cat.CatBuilder::build);

        JsonRead<Animal> oneOfRead = JsonRead.oneOf(_optString("type"), List.of(
                caseOf($Some(API.$("dog")), dogJsonReadV1),
                caseOf($Some(API.$("cat")), catJsonRead),
                caseOf(API.$(), catJsonRead)
        ));

        Animal value1 = oneOfRead.read(Json.obj(
                $$("type", "dog"),
                $$("version", "v1"),
                $$("name", "Brutus")
        )).get();

        Animal value3 = oneOfRead.read(Json.obj(
                $$("type", "cat"),
                $$("version", "v1"),
                $$("name", "Felix")
        )).get();

        assertThat(value1).isEqualTo(new Dog("Brutus"));
        assertThat(value3).isEqualTo(new Cat("Felix"));

        assertThat(oneOfRead.jsonSchema()).isEqualTo(
                JsonSchema.oneOf(List(
                        dogJsonReadV1.jsonSchema(),
                        catJsonRead.jsonSchema()
                ))
        );
    }

    @Test
    public void readPojo() {

        println(json);
        JsResult<Pojo> read = pojoJsonReadV1.read(json);

        assertThat(read).isEqualTo(JsResult.success(Pojo.builder()
                .aString("A string")
                .optLocalDate(Option.none())
                .pojos(List.of(new Pojo2("A value", null)))
                .optLocalDate(Option.of(aDate))
                .build()
        ));
    }

    @Test
    public void pojoJsonSchema() {

        JsonNode jsonSchemaAsJson = jsonSchemaObject.toJson();
        JSONObject jsonObjectSchema = new JSONObject(Json.stringify(jsonSchemaAsJson));
        Schema schema = SchemaLoader.load(jsonObjectSchema);
        schema.validate(new JSONObject(Json.stringify(json)));
        println(jsonSchemaAsJson);
        assertThat(jsonSchemaAsJson).isEqualTo(expectedJsonSchema);
    }

    @Test
    public void readPojoInvalid() {

        Supplier<JsonRead<Pojo3>> pojo3Read = () -> {
            Pojo3.Pojo3Builder builder3 = Pojo3.builder();
            return _string("value", builder3::value).map(Pojo3.Pojo3Builder::build);
        };

        JsonRead<Pojo2> pojo2Read = _string("value", Pojo2.builder()::value)
                .and(_opt("inner", pojo3Read.get()), Pojo2.Pojo2Builder::inner)
                .map(Pojo2.Pojo2Builder::build);

        JsonRead<Pojo> jsRead = _string("aString", Pojo.builder()::aString)
                .and(_optString("optString"), Pojo.PojoBuilder::optString)
                .and(_list("pojos", pojo2Read), Pojo.PojoBuilder::pojos)
                .and(_opt("optLocalDate", _isoLocalDate()), Pojo.PojoBuilder::optLocalDate)
                .and(_opt("inner", pojo3Read.get()), Pojo.PojoBuilder::inner)
                .map(Pojo.PojoBuilder::build);

        LocalDate aDate = LocalDate.now();

        ObjectNode json = Json.obj(
                $("aString", 0),
                $("pojos", Json.arr(
                        Json.obj($("value", "A value"), $("inner", Json.obj($("valuessss", 0))))
                )),
                $("inner", Json.obj($("value", 0))),
                $("optLocalDate", DateTimeFormatter.ISO_LOCAL_DATE.format(aDate))
        );
        println(json);
        JsResult<Pojo> read = jsRead.read(json);

        assertThat(read).isEqualTo(JsResult.error(
                JsResult.JsError.Error.error("aString", "string.expected"),
                JsResult.JsError.Error.error("pojos[0].inner.value", "path.not.found"),
                JsResult.JsError.Error.error("inner.value", "string.expected")
        ));
    }

    @Test
    public void readFromClass() {
        JsonRead<Pojo> pojoJsonRead = _fromClass(Pojo.class);
        JsResult<Pojo> read = pojoJsonRead.read(json);
        assertThat(pojoJsonRead.jsonSchema()).isEqualTo(JsonSchema.emptySchema());
        assertThat(read).isEqualTo(JsResult.success(
                Pojo.builder()
                        .aString("A string")
                        .optLocalDate(Option.none())
                        .pojos(List.of(new Pojo2("A value", null)))
                        .optLocalDate(Option.of(aDate))
                        .build()
        ));
    }

    @Test
    public void readFromTypeReference() {
        JsonRead<List<Pojo>> pojoJsonRead = _fromClass(new TypeReference<List<Pojo>>() {});
        JsResult<List<Pojo>> read = pojoJsonRead.read(Json.arr(json));
        assertThat(pojoJsonRead.jsonSchema()).isEqualTo(JsonSchema.emptySchema());
        assertThat(read).isEqualTo(JsResult.success(List(
                Pojo.builder()
                        .aString("A string")
                        .optLocalDate(Option.none())
                        .pojos(List.of(new Pojo2("A value", null)))
                        .optLocalDate(Option.of(aDate))
                        .build()
        )));
    }

    @Data
    @Builder
    public static class Pojo {
        public String aString;
        public Option<String> optString;
        public List<Pojo2> pojos;
        public Option<LocalDate> optLocalDate;
        public Option<Pojo3> inner;

        public Pojo(String aString, Option<String> optString, List<Pojo2> pojos, Option<LocalDate> optLocalDate, Option<Pojo3> inner) {
            this.aString = aString;
            this.optString = ensureOption(optString);
            this.pojos = pojos;
            this.optLocalDate = ensureOption(optLocalDate);
            this.inner = ensureOption(inner);
        }

        public Pojo() {
            this(null, null, null, null, null);
        }

    }


    public interface Animal { }

    @Builder
    @Value
    public static class Dog implements Animal {
        String name;
    }

    @Builder
    @Value
    public static class Cat implements Animal {
        String name;
    }

    @Data
    @Builder
    public static class Pojo2 {
        public String value;
        public Option<Pojo3> inner;

        public Pojo2(String value, Option<Pojo3> inner) {
            this.value = value;
            this.inner = ensureOption(inner);
        }

        public Pojo2() {
            this(null, null);
        }
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Pojo3 {
        String value;
    }

    public enum TestEnum {
        test1, test2
    }

    private static <A> Option<A>ensureOption(Option<A> opt) {
        return opt == null ? Option.none() : opt;
    }

}