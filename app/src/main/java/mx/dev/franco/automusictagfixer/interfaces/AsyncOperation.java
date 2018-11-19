package mx.dev.franco.automusictagfixer.interfaces;

public interface AsyncOperation<P, T> {
    void onStartAsyncOperation(P params);
    void onFinishedOperation(T result);
}
