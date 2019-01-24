package mx.dev.franco.automusictagfixer.interfaces;

public interface AsyncOperation<P, R, C, E> {
    void onAsyncOperationStarted(P params);
    void onAsyncOperationFinished(R result);
    void onAsyncOperationCancelled(C cancellation);
    void onAsyncOperationError(E error);
}
