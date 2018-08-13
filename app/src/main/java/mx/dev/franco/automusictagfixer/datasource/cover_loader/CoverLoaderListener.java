package mx.dev.franco.automusictagfixer.datasource.cover_loader;

public interface CoverLoaderListener {
    void onLoadingStart();
    void onLoadingFinished(byte[] cover);
    void onLoadingCancelled();
    void onLoadingError(String error);
}
