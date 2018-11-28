package mx.dev.franco.automusictagfixer.interfaces;

public interface AsyncOperation<P, T, C, E> {
    void onAsyncOperationStarted(P params);
    void onAsyncOperationFinished(T result);
    void onAsyncOperationCancelled(C cancellation);
    void onAsyncOperationError(E error);
}
