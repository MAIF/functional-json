package fr.maif.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.vavr.collection.List;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static fr.maif.json.JsonWrite.*;
import static fr.maif.json.Json.$$;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonWriteTest {

    @Test
    public void writeLocalDateIso() {
        JsonNode write = $localdate().write(LocalDate.of(2019, 2, 15));
        assertThat(write).isEqualTo(new TextNode("2019-02-15"));
    }

    @Test
    public void writeLocalDate() {
        JsonNode write = $localdate(DateTimeFormatter.BASIC_ISO_DATE).write(LocalDate.of(2019, 2, 15));
        assertThat(write).isEqualTo(new TextNode("20190215"));
    }

    @Test
    public void writeLocalDateTimeIso() {
        JsonNode write = $localdatetime().write(LocalDateTime.of(2019, 2, 15, 0, 0, 0));
        assertThat(write).isEqualTo(new TextNode("2019-02-15T00:00:00"));
    }

    @Test
    public void writeLocalDateTime() {
        JsonNode write = $localdatetime(DateTimeFormatter.BASIC_ISO_DATE).write(LocalDateTime.of(2019, 2, 15, 0, 0, 0));
        assertThat(write).isEqualTo(new TextNode("20190215"));
    }

    @Test
    public void writeString() {
        JsonNode write = $string().write("A string");
        assertThat(write).isEqualTo(new TextNode("A string"));
    }

    @Test
    public void writeBoolean() {
        JsonNode write = $boolean().write(Boolean.TRUE);
        assertThat(write).isEqualTo(BooleanNode.TRUE);
    }

    @Test
    public void writeInt() {
        JsonNode write = $int().write(1);
        assertThat(write).isEqualTo(new IntNode(1));
    }

    @Test
    public void writeBigDecimal() {
        JsonNode write = $bigdecimal().write(BigDecimal.valueOf(5000, 3));
        assertThat(write).isEqualTo(new TextNode("5.00"));
    }

    @Test
    public void writeEnum() {
        JsonNode write = JsonWrite.<TestEnum>$enum().write(TestEnum.test1);
        assertThat(write).isEqualTo(new TextNode("test1"));
    }

    @Test
    public void writeJsonArray() {
        JsonNode write = $jsonArray().write(Json.arr("1"));
        assertThat(write).isEqualTo(Json.arr("1"));
    }
    @Test
    public void writeJsonObject() {
        JsonNode write = $jsonObject().write(Json.obj($$("key", "value")));
        assertThat(write).isEqualTo(Json.obj($$("key", "value")));
    }

    @Test
    public void writeList() {
        JsonNode write = $list($string()).write(List.of("1"));
        assertThat(write).isEqualTo(Json.arr("1"));
    }

    @Test
    public void writeNullList() {
        JsonNode write = $list($string()).write(null);
        assertThat(write).isEqualTo(Json.newArray());
    }


    public enum TestEnum {
        test1
    }
}