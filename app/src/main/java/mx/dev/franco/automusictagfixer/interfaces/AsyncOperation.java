package mx.dev.franco.automusictagfixer.interfaces;

/**
 * @author Franco Castillo
 * An interface to send events when an async task will be processed.
 * @param <P> The type of input.
 * @param <R> The type of result.
 * @param <C> The type of result when cancelled.
 * @param <E> The type of error.
 */
public interface AsyncOperation<P, R, C, E> {
    default void onAsyncOperationStarted(P params){}
    default void onAsyncOperationFinished(R result){}
    default void onAsyncOperationCancelled(C cancellation){}
    default void onAsyncOperationError(E error){}
}
