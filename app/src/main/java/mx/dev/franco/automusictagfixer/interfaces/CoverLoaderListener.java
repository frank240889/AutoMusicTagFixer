package mx.dev.franco.automusictagfixer.interfaces;

public interface CoverLoaderListener {
    void onLoadingStart();
    void onLoadingFinished(byte[] cover);
    void onLoadingCancelled();
    void onLoadingError(String error);
}
