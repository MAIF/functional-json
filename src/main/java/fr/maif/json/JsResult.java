package fr.maif.json;

import io.vavr.API;
import io.vavr.Tuple;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.function.Function.identity;

/**
 * A JsResult is a sum type that represents a json parsing result.
 * It could be a JsSuccess or a JsError.
 * @param <T>
 */
public interface JsResult<T> {

    static <R, _1 extends R> API.Match.Pattern1<JsResult.JsSuccess<R>, _1> $JsSuccess(API.Match.Pattern<_1, ?> p1) {
        return API.Match.Pattern1.of(JsResult.JsSuccess.class, p1, s -> Tuple.of(s.value));
    }

    static <R> API.Match.Pattern1<JsResult.JsError<R>, Seq<Error>> $JsError(API.Match.Pattern<Seq<Error>, ?> p1) {
        return API.Match.Pattern1.of(JsResult.JsError.class, p1, s -> Tuple.of(s.errors));
    }
    /**
     * Create a JsResult in success from value
     *
     * @param value the value in success
     * @param <T>
     * @return the JsResultin success
     */
    static <T> JsResult<T> success(T value) {
        return new JsSuccess<>(value);
    }

    /**
     * Create a JsResult in error from error list
     *
     * @param errors the list of errors
     * @param <T>
     * @return the JsResult in error
     */
    static <T> JsResult<T> error(Seq<Error> errors) {
        return new JsError<>(errors);
    }

    /**
     * Create a JsResult in error from error list
     *
     * @param errors the list of errors
     * @param <T>
     * @return the JsResult in error
     */
    static <T> JsResult<T> error(JsResult.Error... errors) {
        return new JsError<>(List.of(errors));
    }

    /**
     * @return true if the JsResult is on error
     */
    Boolean isError();

    /**
     * @return true if the JsResult is on success
     */
    default Boolean isSuccess() {
        return !this.isError();
    }

    /**
     * Convert the value in case of success
     * @param func the function to apply in case of success
     * @param <A>
     * @return the JsResult with function applied
     */
    <A> JsResult<A> map(Function<T, A> func);

    /**
     * Convert the error in case of error
     * @param func the function to apply in case of success
     * @return the JsResult with function applied on the error side
     */
    JsResult<T> mapError(Function<Seq<Error>, Seq<Error>> func);

    /**
     * Compose two JsResult. The func is not applied in case of error.
     * @param func the function to apply in case of error
     * @param <A>
     * @return the JsResult
     */
    <A> JsResult<A> flatMap(Function<T, JsResult<A>> func);

    /**
     * @return the errors, empty list in case of success.
     */
    Seq<Error> getErrors();

    /**
     * Convert the JsResult to a vavr Either.
     * @return the either
     */
    Either<Seq<Error>, T> toEither();

    /**
     * Folds either the error or the success side of this disjunction.
     *
     * @param onError maps the error value if this is a JsError
     * @param onSuccess maps the success value if this is a JsSuccess
     * @param <R>
     * @return the value
     */
    default <R> R fold(Function<Seq<Error>, R> onError, Function<T, R> onSuccess) {
        return this.toEither().fold(onError, onSuccess);
    }

    /**
     * Get the success value or throw an exception.
     * This method is unsafe, please use fold or getOrElse instead.
     *
     * @return the success
     * @throws IllegalStateException in case of a JsError
     */
    T get();

    /**
     * Get the success value or default value in case of error.
     *
     * @return the success or default.
     */
    default T getOrElse(Supplier<T> onError) {
        return this.fold(__ -> onError.get(), identity());
    }

    /**
     * Combine this result other.
     *
     * @param other
     * @return
     */
    default JsResult<Seq<T>> combineMany(JsResult<Seq<T>> other) {
        if (this.isSuccess() && other.isSuccess()) {
            JsSuccess<T> _this = (JsSuccess<T>) this;
            JsSuccess<Seq<T>> _other = (JsSuccess<Seq<T>>) other;
            return new JsSuccess<>(_other.value.append(_this.value));
        } else if (this.isError() && other.isError()) {
            JsError<T> _this = (JsError<T>) this;
            JsError<Seq<T>> _other = (JsError<Seq<T>>) other;
            return new JsError<>(_this.errors.appendAll(_other.errors));
        } else if (this.isError() && other.isSuccess()) {
            JsError<T> _this = (JsError<T>) this;
            return new JsError<>(_this.errors);
        } else {
            JsError<Seq<T>> _other = (JsError<Seq<T>>) other;
            return new JsError<>(_other.errors);
        }
    }


    class JsSuccess<T> implements JsResult <T> {
        private final T value;

        public JsSuccess(T value) {
            this.value = value;
        }

        @Override
        public Boolean isError() {
            return false;
        }

        @Override
        public <A> JsResult<A> map(Function<T, A> func) {
            return new JsSuccess<>(func.apply(value));
        }

        @Override
        public <A> JsResult<A> flatMap(Function<T, JsResult<A>> func) {
            return func.apply(value);
        }

        @Override
        public JsResult<T> mapError(Function<Seq<Error>, Seq<Error>> func) {
            return this;
        }

        @Override
        public Either<Seq<Error>, T> toEither() {
            return Either.right(value);
        }

        @Override
        public Seq<Error> getErrors() {
            return List.empty();
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", JsSuccess.class.getSimpleName() + "[", "]")
                    .add("value=" + value)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsSuccess<?> jsSuccess = (JsSuccess<?>) o;
            return Objects.equals(value, jsSuccess.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    class JsError<T> implements JsResult <T> {

        private final Seq<Error> errors;

        public JsError(Seq<Error> errors) {
            this.errors = errors;
        }

        @Override
        public Boolean isError() {
            return true;
        }

        @Override
        public <A> JsResult<A> map(Function<T, A> func) {
            return new JsError<>(errors);
        }

        @Override
        public <A> JsResult<A> flatMap(Function<T, JsResult<A>> func) {
            return new JsError<>(errors);
        }

        @Override
        public JsResult<T> mapError(Function<Seq<Error>, Seq<Error>> func) {
            return new JsError<>(func.apply(errors));
        }

        @Override
        public Either<Seq<Error>, T> toEither() {
            return Either.left(errors);
        }

        @Override
        public Seq<Error> getErrors() {
            return errors;
        }

        @Override
        public T get() {
            throw new IllegalStateException("JsError, no value present: "+errors);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsError<?> jsError = (JsError<?>) o;
            return Objects.equals(errors, jsError.errors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errors);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", JsError.class.getSimpleName() + "[", "]")
                    .add("errors=" + errors)
                    .toString();
        }
    }


    class Error {

        public final Option<String> path;
        public final String[] args;
        public final String message;

        public Error(Option<String> path, String message, String[] args) {
            this.path = path;
            this.args = args;
            this.message = message;
        }

        public static Error error(String path, String message, Object... args) {
            return new Error(Option.of(path), message, List.of(args).map(Object::toString).toJavaArray(String[]::new));
        }

        public static Error error(String message, Object... args) {
            return new Error(Option.none(), message, List.of(args).map(Object::toString).toJavaArray(String[]::new));
        }

        public Error repath(String path) {
            return new Error(this.path.map(p -> path+"."+p).orElse(Option.of(path)), message, args);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Error.class.getSimpleName() + "[", "]")
                    .add("path=" + path)
                    .add("args=" + Arrays.toString(args))
                    .add("message='" + message + "'")
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Error error = (Error) o;
            return Objects.equals(path, error.path) &&
                    Arrays.equals(args, error.args) &&
                    Objects.equals(message, error.message);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(path, message);
            result = 31 * result + Arrays.hashCode(args);
            return result;
        }
    }
}
