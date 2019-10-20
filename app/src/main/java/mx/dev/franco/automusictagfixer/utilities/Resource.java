package mx.dev.franco.automusictagfixer.utilities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Generic holder that represents an object with state.
 * @param <T> The type of object.
 * @author Taken from google examples.
 */
public class Resource<T> {
    public final Status status;
    public final T data;

    private Resource(@NonNull Status status, @Nullable T data){
        this.status = status;
        this.data = data;
    }

    public static <T> Resource<T> success(@NonNull T data){
        return new Resource<>(Status.SUCCESS, data);
    }

    public static <T> Resource<T> error(@Nullable T data){
        return new Resource<>(Status.ERROR, data);
    }

    public static <T> Resource<T> loading(@Nullable T data){
        return new Resource<>(Status.LOADING, data);
    }

    public static <T> Resource<T> cancelled(@Nullable T data) {
        return new Resource<>(Status.CANCELLED, data);
    }

    public enum Status {
        SUCCESS, ERROR, LOADING, CANCELLED
    }
}
