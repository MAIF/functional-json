package fr.maif.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static fr.maif.json.Json.$$;
import static fr.maif.json.JsonWrite.$list;
import static fr.maif.json.JsonWrite.$string;
import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static java.util.function.Function.identity;

public interface JsonSchema {

    EmptySchema EMPTY_SCHEMA = new EmptySchema();
    StringSchema STRING_SCHEMA = new StringSchema();
    IntegerSchema INTEGER_SCHEMA = new IntegerSchema();
    NumberSchema NUMBER_SCHEMA = new NumberSchema();
    DateTimeSchema DATE_TIME_SCHEMA = new DateTimeSchema();
    DateSchema DATE_SCHEMA = new DateSchema();
    BooleanSchema BOOLEAN_SCHEMA = new BooleanSchema();


    ObjectNode toJson();

    default RootSchema toRoot() {
        if (this instanceof RootSchema) {
            return (RootSchema) this;
        } else {
            return RootSchema.empty(this);
        }
    }

    static EmptySchema emptySchema() {
        return EMPTY_SCHEMA;
    }

    static StringSchema stringSchema() {
        return STRING_SCHEMA;
    }

    static <E extends Enum<E>> EnumSchema<E> enumSchema(Class<E> clazz) {
        return new EnumSchema<E>(clazz);
    }

    static EnumSchemaFromStrings enumSchema(List<String> values) {
        return new EnumSchemaFromStrings(values);
    }

    static DateTimeSchema dateTimeSchema() {
        return DATE_TIME_SCHEMA;
    }

    static JsonSchema dateSchema() {
        return DATE_SCHEMA;
    }

    static BooleanSchema booleanSchema() {
        return BOOLEAN_SCHEMA;
    }

    static JsonSchema numberSchema() {
        return NUMBER_SCHEMA;
    }

    static JsonSchema integerSchema() {
        return INTEGER_SCHEMA;
    }

    static ArraySchema arraySchema() {
        return new ArraySchema(null);
    }

    static ArraySchema arraySchema(JsonSchema items) {
        return new ArraySchema(items);
    }

    static ObjectSchema objectSchema() {
        return new ObjectSchema(List.empty());
    }

    static ObjectSchema objectSchema(List<Property> properties) {
        return new ObjectSchema(properties);
    }
    static ObjectSchema objectSchema(Property... properties) {
        return new ObjectSchema(List(properties));
    }

    static ObjectSchema objectSchema(String field, JsonSchema type) {
        return new ObjectSchema(List(propertySchema(field, type)));
    }

    default JsonSchema computeSchema() {
        if (this instanceof Property) {
            return new ObjectSchema(List((Property) this));
        }  else {
            return this;
        }
    }

    static ObjectSchema objectSchema(String field, JsonSchema type, Boolean required) {
        return new ObjectSchema(List(propertySchema(field, type).withRequired(required)));
    }

    static DefinitionsSchema definitionsSchema(List<Property> properties) {
        return new DefinitionsSchema(properties);
    }

    static RefSchema refSchema(String ref) {
        return new RefSchema(ref);
    }

    static Property propertySchema(String field, JsonSchema... type) {
        return new Property(field, Option.of(type).map(List::of).toList().flatMap(identity()), true);
    }

    static Property propertySchema(String field, List<JsonSchema> types) {
        return new Property(field, types, true);
    }

    static OneOfSchema oneOf(List<JsonSchema> schemas) {
        return new OneOfSchema(schemas);
    }

    static OneOfSchema oneOf(JsonSchema... schemas) {
        return new OneOfSchema(List(schemas));
    }

    static AnyOfSchema anyOf(List<JsonSchema> schemas) {
        return new AnyOfSchema(schemas);
    }

    static AllOfSchema allOf(List<JsonSchema> schemas) {
        return new AllOfSchema(schemas);
    }

    default JsonSchema and(JsonSchema jsonSchema) {
        return Match(this).of(
                Case($(instanceOf(EmptySchema.class)), obj -> jsonSchema),
                Case($(instanceOf(ObjectSchema.class)), obj -> obj.and(jsonSchema)),
                Case($(instanceOf(Property.class)), obj -> new ObjectSchema(List.of(obj)).and(jsonSchema)),
                Case($(), __ -> this)
        );
    }

    default JsonSchema id(String id) {
        return this.toRoot().withId(id);
    }

    default JsonSchema title(String title) {
        return this.toRoot().withTitle(title);
    }

    default JsonSchema description(String description) {
        return this.toRoot().withDescription(description);
    }

    default JsonSchema schema(String schema) {
        return this.toRoot().withSchema(schema);
    }

    default JsonSchema exemples(List<JsonNode> exemples) {
        return this.toRoot().withExemples(exemples);
    }

    @ToString
    @EqualsAndHashCode
    class RootSchema implements JsonSchema {

        private final Option<String> schema;
        private final Option<String> id;
        private final Option<String> title;
        private final Option<String> description;
        private final List<JsonNode> exemples;
        private final JsonSchema embeddedSchema;

        public RootSchema(Option<String> schema, Option<String> id, Option<String> title, Option<String> description, List<JsonNode> exemples, JsonSchema embeddedSchema) {
            this.schema = schema == null ? Option.none(): schema;
            this.id = id == null ? Option.none(): id;
            this.title = title == null ? Option.none(): title;
            this.description = description == null ? Option.none(): description;
            this.exemples = exemples == null ? List.empty(): exemples;
            this.embeddedSchema = embeddedSchema;
        }

        public static RootSchema empty(JsonSchema embeddedSchema) {
            return new RootSchema(Option.none(), Option.none(), Option.none(), Option.none(), List.empty(), embeddedSchema);
        }

        public RootSchema withSchema(String schema) {
            if (embeddedSchema instanceof RootSchema) {
                return ((RootSchema)embeddedSchema).withSchema(schema);
            }
            return new RootSchema(Option.of(schema), id, title, description, exemples, embeddedSchema);
        }

        public RootSchema withId(String id) {
            if (embeddedSchema instanceof RootSchema) {
                return ((RootSchema)embeddedSchema).withId(id);
            }
            return new RootSchema(schema, Option.of(id), title, description, exemples, embeddedSchema);
        }

        public RootSchema withTitle(String title) {
            if (embeddedSchema instanceof RootSchema) {
                return ((RootSchema)embeddedSchema).withTitle(title);
            }
            return new RootSchema(schema, id, Option.of(title), description, exemples, embeddedSchema);
        }

        public RootSchema withDescription(String description) {
            if (embeddedSchema instanceof RootSchema) {
                return ((RootSchema)embeddedSchema).withDescription(description);
            }
            return new RootSchema(schema, id, title, Option.of(description), exemples, embeddedSchema);
        }

        public RootSchema withExemples(List<JsonNode> exemples) {
            if (embeddedSchema instanceof RootSchema) {
                return ((RootSchema)embeddedSchema).withExemples(exemples);
            }
            return new RootSchema(schema, id, title, description, Option.of(exemples).toList().flatMap(identity()), embeddedSchema);
        }

        @Override
        public ObjectNode toJson() {
            ObjectNode currentSchema = embeddedSchema.toJson();
            return Json.obj(currentSchema,
                    $$("$schema", schema),
                    $$("$id", id),
                    $$("title", title),
                    $$("description", description),
                    $$("exemples", Json.arr(exemples.toJavaArray(JsonNode[]::new)))
            );
        }

    }

    @ToString
    @EqualsAndHashCode
    class EmptySchema implements JsonSchema {
        @Override
        public ObjectNode toJson() {
            return Json.obj();
        }
    }

    @ToString
    @EqualsAndHashCode
    class StringSchema implements JsonSchema {
        @Override
        public ObjectNode toJson() {
            return Json.obj($$("type", "string"));
        }
    }

    @ToString
    @EqualsAndHashCode
    class OneOfSchema implements JsonSchema {
        private final List<JsonSchema> schemas;

        public OneOfSchema(List<JsonSchema> schemas) {
            this.schemas = schemas.distinct();
        }

        @Override
        public ObjectNode toJson() {
            return Json.obj($$("oneOf", Json.arr(schemas.map(JsonSchema::toJson))));
        }

    }

    @ToString
    @EqualsAndHashCode
    class AnyOfSchema implements JsonSchema {
        private final List<JsonSchema> schemas;

        public AnyOfSchema(List<JsonSchema> schemas) {
            this.schemas = schemas;
        }

        @Override
        public ObjectNode toJson() {
            return Json.obj($$("anyOf", Json.arr(schemas.map(JsonSchema::toJson))));
        }
    }

    @ToString
    @EqualsAndHashCode
    class AllOfSchema implements JsonSchema {
        private final List<JsonSchema> schemas;

        public AllOfSchema(List<JsonSchema> schemas) {
            this.schemas = schemas;
        }

        @Override
        public ObjectNode toJson() {
            return Json.obj($$("allOf", Json.arr(schemas.map(JsonSchema::toJson))));
        }

    }

    @ToString
    @EqualsAndHashCode
    class EnumSchema<E extends Enum<E>> implements JsonSchema {

        private final Class<E> enumClass;

        public EnumSchema(Class<E> enumClass) {
            this.enumClass = enumClass;
        }


        @Override
        public ObjectNode toJson() {
            return Json.obj(
                    $$("type", "string"),
                    $$("enum", Json.arr(List.of(enumClass.getEnumConstants()).map(Enum::name).map(TextNode::new)))
            );
        }
    }

    @ToString
    @EqualsAndHashCode
    class EnumSchemaFromStrings implements JsonSchema {

        private final List<String> values;

        public EnumSchemaFromStrings(List<String> values) {
            this.values = values;
        }

        @Override
        public ObjectNode toJson() {
            return Json.obj(
                    $$("type", "string"),
                    $$("enum", values, $list($string()))
            );
        }
    }

    @ToString
    @EqualsAndHashCode
    class DateSchema implements JsonSchema {
        @Override
        public ObjectNode toJson() {
            return Json.obj(
                    $$("type", "string"),
                    $$("format", "date")
            );
        }
    }

    @ToString
    @EqualsAndHashCode
    class DateTimeSchema implements JsonSchema {
        @Override
        public ObjectNode toJson() {
            return Json.obj(
                    $$("type", "string"),
                    $$("format", "date-time")
            );
        }
    }

    @ToString
    @EqualsAndHashCode
    class BooleanSchema implements JsonSchema {
        @Override
        public ObjectNode toJson() {
            return Json.obj($$("type", "boolean"));
        }
    }

    @ToString
    @EqualsAndHashCode
    class IntegerSchema implements JsonSchema {
        @Override
        public ObjectNode toJson() {
            return Json.obj($$("type", "integer"));
        }
    }

    @ToString
    @EqualsAndHashCode
    class NumberSchema implements JsonSchema {
        @Override
        public ObjectNode toJson() {
            return Json.obj($$("type", "number"));
        }
    }

    @ToString
    @EqualsAndHashCode
    class ArraySchema implements JsonSchema {

        private final JsonSchema items;

        public ArraySchema(JsonSchema items) {
            this.items = items;
        }

        @Override
        public ObjectNode toJson() {
            if (items == null) {
                return Json.obj($$("type", "array"));
            } else {
                return Json.obj(
                        $$("type", "array"),
                        $$("items", this.items.toJson())
                );
            }
        }
    }

    @ToString
    @EqualsAndHashCode
    class ObjectSchema implements JsonSchema {

        private final List<Property> properties;

        public ObjectSchema(List<Property> properties) {
            this.properties = properties;
        }

        public ObjectSchema and(JsonSchema jsonSchema) {
            return Match(jsonSchema).of(
                Case($(instanceOf(ObjectSchema.class)), obj -> new ObjectSchema(properties.appendAll(obj.properties))),
                Case($(instanceOf(Property.class)), obj -> new ObjectSchema(properties.append(obj))),
                Case($(), __ -> this)
            );
        }

        @Override
        public ObjectNode toJson() {
            if (properties.isEmpty()) {
                return Json.obj(
                        $$("type", "object")
                );
            } else {
                List<String> required = properties.filter(p -> p.required).map(p -> p.name);
                return Json.obj(
                        $$("type", "object"),
                        $$("required", Json.arr(required.sorted().toJavaArray(String[]::new))),
                        $$("properties", properties.map(Property::toJson).fold(Json.obj(), Json::merge))
                );
            }
        }
    }

    @ToString
    @EqualsAndHashCode
    class DefinitionsSchema implements JsonSchema {
        private final List<Property> properties;

        public DefinitionsSchema(List<Property> properties) {
            this.properties = properties;
        }

        @Override
        public ObjectNode toJson() {
            if (properties.isEmpty()) {
                return Json.obj(
                        $$("definitions", Json.obj())
                );
            } else {
                return Json.obj(
                        $$("definitions", properties.map(Property::toJson).fold(Json.obj(), Json::merge))
                );
            }
        }
    }

    @ToString
    @EqualsAndHashCode
    class RefSchema implements JsonSchema {
        private final String ref;

        public RefSchema(String ref) {
            this.ref = ref;
        }

        @Override
        public ObjectNode toJson() {
            return Json.obj($$("$ref", ref));
        }
    }

    @ToString
    @EqualsAndHashCode
    class Property implements JsonSchema {

        private final String name;
        private final List<JsonSchema> schema;
        private final Boolean required;

        public Property(String name, List<JsonSchema> schema, Boolean required) {
            this.name = name;
            this.schema = schema;
            this.required = required;
        }

        public Property withRequired(Boolean required) {
            return new Property(name, schema, required);
        }

        public Property notRequired() {
            return new Property(name, schema, false);
        }

        @Override
        public Property id(String id) {
            return new Property(name, schema.map(s -> s.id(id)), required);
        }

        @Override
        public Property title(String title) {
            return new Property(name, schema.map(s -> s.title(title)), required);
        }

        @Override
        public Property description(String description) {
            return new Property(name, schema.map(s -> s.description(description)), required);
        }

        @Override
        public Property schema(String schema) {
            return new Property(name, this.schema.map(s -> s.schema(schema)), required);
        }

        @Override
        public Property exemples(List<JsonNode> exemples) {
            return new Property(name, schema.map(s -> s.exemples(exemples)), required);
        }

        @Override
        public ObjectNode toJson() {

            if (schema.length() == 1) {
                return Json.obj($$(name, schema.head().toJson()));
            } else {
                return Json.obj($$(name, Json.arr(schema.map(JsonSchema::toJson).toJavaArray(ObjectNode[]::new))));
            }

        }
    }

}
