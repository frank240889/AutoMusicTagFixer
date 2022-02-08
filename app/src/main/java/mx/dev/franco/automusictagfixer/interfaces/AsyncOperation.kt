package mx.dev.franco.automusictagfixer.interfaces

/**
 * @author Franco Castillo
 * An interface to send events when an async task will be processed.
 * @param <P> The type of input.
 * @param <R> The type of result.
 * @param <C> The type of result when cancelled.
 * @param <E> The type of error.
</E></C></R></P> */
interface AsyncOperation<P, R, C, E> {
    fun onAsyncOperationStarted(params: P) {}
    fun onAsyncOperationFinished(result: R) {}
    fun onAsyncOperationCancelled(cancellation: C) {}
    fun onAsyncOperationError(error: E) {}
}