package fr.maif.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.vavr.collection.Traversable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A functional interface to describe a T to json writer.
 *
 * <pre>{@code
 *
 * }</pre>
 * @param <T>
 */
@FunctionalInterface
public interface JsonWrite<T> {

    JsonNode write(T value);

    static <T> JsonWrite<T> auto() {
        return t -> Json.mapper().valueToTree(t);
    }

    /**
     * Write a date to json with the good format
     *
     * @param formatter the date formatter
     * @return the writer
     */
    static JsonWrite<LocalDate> $localdate(DateTimeFormatter formatter) {
        return localdate -> new TextNode(formatter.format(localdate));
    }


    /**
     * Write a date to json with the good format
     *
     * @param formatter the date formatter
     * @return the writer
     */
    static JsonWrite<LocalDateTime> $localdatetime(DateTimeFormatter formatter) {
        return localdate -> new TextNode(formatter.format(localdate));
    }

    /**
     * Write a date to json in ISO
     *
     * @return the writer
     */
    static JsonWrite<LocalDate> $localdate() {
        return $localdate(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Write a date to json in ISO
     *
     * @return the writer
     */
    static JsonWrite<LocalDateTime> $localdatetime() {
        return $localdatetime(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Write string to json
     *
     * @return the writer
     */
    static JsonWrite<String> $string() {
        return TextNode::new;
    }

    /**
     * Write boolean to json
     *
     * @return the writer
     */
    static JsonWrite<Boolean> $boolean() {
        return BooleanNode::valueOf;
    }

    /**
     * Write int to json
     *
     * @return the writer
     */
    static JsonWrite<Integer> $int() {
        return IntNode::new;
    }

    /**
     * Write long to json
     * @return
     */
    static JsonWrite<Long> $long() {
        return LongNode::new;
    }
        /**
     * Write double to json
     * @return
     */
    static JsonWrite<Double> $double() {
        return DoubleNode::new;
    }
        /**
     * Write float to json
     * @return
     */
    static JsonWrite<Float> $float() {
        return FloatNode::new;
    }

    /**
     * Write big decimal to json
     *
     * @return the writer
     */
    static JsonWrite<BigDecimal> $bigdecimal() {
        return value -> new TextNode(value.setScale(2, RoundingMode.HALF_UP).toString());
    }

    /**
     * Write json object to json
     *
     * @return the writer
     */
    static JsonWrite<ObjectNode> $jsonObject() {
        return n -> n;
    }

    /**
     * Write json array to json
     *
     * @return the writer
     */
    static JsonWrite<ArrayNode> $jsonArray() {
        return n -> n;
    }

    /**
     * Write enum to json
     *
     * @return the writer
     */
    static <T extends Enum<T>> JsonWrite<T> $enum() {
        return en -> new TextNode(en.name());
    }

    /**
     * Write list to json
     *
     * @param write the writer
     * @return the writer
     */
    static <T, Trav extends Traversable<T>> JsonWrite<Trav> $list(JsonWrite<T> write) {
        return list -> {
            if (list == null) {
                return Json.newArray();
            }
            return Json.arr(list.map(write::write).toJavaArray(JsonNode[]::new));
        };
    }


}
