package mx.dev.franco.automusictagfixer.services.Fixer;

public interface DataTrackLoader<T> {
    void onTrackDataLoaded(T data);
}
