package mx.dev.franco.automusictagfixer.interfaces;

/**
 * Interface to receive the list
 * of tracks to correct.
 * @param <DATA> The type of data to load.
 */
public interface TrackListLoader<DATA>{
    void onDataLoaded(DATA data);
}