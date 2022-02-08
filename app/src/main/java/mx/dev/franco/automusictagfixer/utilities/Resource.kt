package mx.dev.franco.automusictagfixer.utilities

/**
 * Generic holder that represents an object with state.
 * @param <T> The type of object.
 * @author Taken from google examples.
</T> */
class Resource<T> private constructor(val status: Status, val data: T?) {

    enum class Status {
        SUCCESS, ERROR, LOADING, CANCELLED
    }

    companion object {
        @JvmStatic
        fun <T> success(data: T): Resource<T?> {
            return Resource(Status.SUCCESS, data)
        }

        @JvmStatic
        fun <T> error(data: T?): Resource<T?> {
            return Resource(Status.ERROR, data)
        }

        fun <T> loading(data: T?): Resource<T?> {
            return Resource(Status.LOADING, data)
        }

        fun <T> cancelled(data: T?): Resource<T?> {
            return Resource(Status.CANCELLED, data)
        }
    }

}