package fr.maif.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonTest {
    static class Viking {
        public String name;
    }

    @Test
    public void readShouldReadObject() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"name\": \"lodbrok\"}";

        assertThat(Json.fromJson(mapper.readTree(json), Viking.class).get().name).isEqualTo("lodbrok");
    }

    @Test
    public void readShouldReadParametrizedTypes() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = "[{\"name\": \"lodbrok\"}, {\"name\": \"haraldson\"}]";

        TypeReference<List<Viking>> typeReference = new TypeReference<>() {
        };

        List<String> names = Json.fromJson(mapper.readTree(json), typeReference).get().stream().map(v -> v.name).collect(Collectors.toList());

        assertThat(names).containsExactly("lodbrok", "haraldson");
    }
}
