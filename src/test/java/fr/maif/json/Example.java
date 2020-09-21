package fr.maif.json;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.junit.Test;

import static fr.maif.json.Json.$;
import static fr.maif.json.JsonRead._opt;
import static fr.maif.json.JsonRead._string;
import static io.vavr.API.Some;

public class Example {

    @FieldNameConstants
    @Builder
    @AllArgsConstructor
    public static class Viking {

        public static JsonFormat<Viking> format() {
            return JsonFormat.of(reader(), writer());
        }

        public static JsonRead<Viking> reader() {
            return _string(Fields.firstName, Viking.builder()::firstName)
                    .and(_string(Fields.lastName), Viking.VikingBuilder::lastName)
                    .and(_opt(Fields.city, _string()), Viking.VikingBuilder::city)
                    .map(Viking.VikingBuilder::build);
        }

        public static JsonWrite<Viking> writer() {
            return viking -> Json.obj(
                    $(Fields.firstName, viking.firstName),
                    $(Fields.lastName, viking.lastName),
                    $(Fields.city, viking.city)
            );
        }

        public final String firstName;
        public final String lastName;
        public final Option<String> city;
    }



    @Test
    public void test() {

        Viking viking = Viking.builder()
                .firstName("Ragnar")
                .lastName("Lodbrock")
                .city(Some("Kattegat"))
                .build();

        JsonNode jsonNode = Json.toJson(viking, Viking.format());
        String stringify = Json.stringify(jsonNode);

        JsonNode parsed = Json.parse(stringify);

        JsResult<Viking> vikingJsResult = Json.fromJson(parsed, Viking.format());

    }


}
