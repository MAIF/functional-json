package fr.maif.json;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.collection.List;
import org.junit.Test;

import static fr.maif.json.Json.$$;
import static fr.maif.json.JsonSchema.*;
import static io.vavr.API.List;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonSchemaTest {


    @Test
    public void string() {
        JsonSchema jsonSchema = stringSchema();
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj($$("type", "string")));
    }

    private enum TestEnum {
        value1, value2;
    }

    @Test
    public void enumSchemaSpec() {
        JsonSchema jsonSchema = enumSchema(TestEnum.class);
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("type", "string"),
                $$("enum", Json.arr(List(TestEnum.values()).map(TestEnum::name).toJavaArray(String[]::new)))
        ));
    }

    @Test
    public void integer() {
        JsonSchema jsonSchema = JsonSchema.integerSchema();
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj($$("type", "integer")));
    }

    @Test
    public void number() {
        JsonSchema jsonSchema = JsonSchema.numberSchema();
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj($$("type", "number")));
    }

    @Test
    public void booleanSchema() {
        JsonSchema jsonSchema = JsonSchema.booleanSchema();
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj($$("type", "boolean")));
    }

    @Test
    public void date() {
        JsonSchema jsonSchema = JsonSchema.dateSchema();
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj($$("type", "string"), $$("format", "date")));
    }

    @Test
    public void dateTime() {
        JsonSchema jsonSchema = JsonSchema.dateTimeSchema();
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj($$("type", "string"), $$("format", "date-time")));
    }

    @Test
    public void array() {
        JsonSchema jsonSchema = JsonSchema.arraySchema();
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj($$("type", "array")));
    }
    @Test
    public void typedArray() {
        JsonSchema jsonSchema = JsonSchema.arraySchema(stringSchema());
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("type", "array"),
                $$("items", Json.obj($$("type", "string")))
        ));
    }

    @Test
    public void object() {
        JsonSchema jsonSchema = objectSchema();
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("type", "object")
        ));
    }

    @Test
    public void objectProperties() {
        JsonSchema jsonSchema = objectSchema(List.of(propertySchema("field", stringSchema())));
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("type", "object"),
                $$("required", Json.arr("field")),
                $$("properties", Json.obj(
                        $$("field", Json.obj(
                                $$("type", "string")
                        ))
                ))
        ));
    }
    
    @Test
    public void objectPropertiesOptional() {
        JsonSchema jsonSchema = objectSchema(List.of(propertySchema("field", stringSchema()).notRequired()));
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("type", "object"),
                $$("required", Json.newArray()),
                $$("properties", Json.obj(
                        $$("field", Json.obj(
                                $$("type", "string")
                        ))
                ))
        ));
    }

    @Test
    public void definitionSchema() {
        JsonSchema jsonSchema = definitionsSchema(List.of(propertySchema("field", stringSchema()).notRequired()));
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("definitions", Json.obj(
                        $$("field", Json.obj(
                                $$("type", "string")
                        ))
                ))
        ));
    }

    @Test
    public void ref() {
        JsonSchema jsonSchema = refSchema("ref");
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("$ref", "ref"))
        );
    }


    @Test
    public void oneOfSpec() {
        JsonSchema jsonSchema = oneOf(List(stringSchema(), integerSchema()));
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("oneOf", Json.arr(
                        Json.obj(
                                $$("type", "string")
                        ),
                        Json.obj(
                                $$("type", "integer")
                        )
                )))
        );
    }


    @Test
    public void anyOfSpec() {
        JsonSchema jsonSchema = anyOf(List(stringSchema(), integerSchema()));
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("anyOf", Json.arr(
                        Json.obj(
                                $$("type", "string")
                        ),
                        Json.obj(
                                $$("type", "integer")
                        )
                )))
        );
    }

    @Test
    public void allOffSpec() {
        JsonSchema jsonSchema = allOf(List(stringSchema(), integerSchema()));
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj(
                $$("allOf", Json.arr(
                        Json.obj(
                                $$("type", "string")
                        ),
                        Json.obj(
                                $$("type", "integer")
                        )
                )))
        );
    }


    @Test
    public void andSimpleSpec() {
        JsonSchema jsonSchema = stringSchema().and(integerSchema());
        assertThat(jsonSchema.toJson()).isEqualTo(Json.obj($$("type", "string")));
    }



    @Test
    public void andObjectSpec() {
        JsonSchema jsonSchema = objectSchema("field1", stringSchema()).and(objectSchema(List(propertySchema("field2", stringSchema()))));
        JsonSchema jsonSchema2 = objectSchema(List(propertySchema("field2", stringSchema()))).and(objectSchema("field1", stringSchema()));
        JsonNode expected = Json.obj(
                $$("type", "object"),
                $$("required", Json.arr("field1", "field2")),
                $$("properties", Json.obj(
                        $$("field1", Json.obj(
                                $$("type", "string")
                        )),
                        $$("field2", Json.obj(
                                $$("type", "string")
                        ))
                ))
        );
        assertThat(jsonSchema.toJson()).isEqualTo(expected);
        assertThat(jsonSchema2.toJson()).isEqualTo(expected);
    }



}