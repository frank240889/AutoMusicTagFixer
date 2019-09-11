package mx.dev.franco.automusictagfixer.modelsUI.track_detail;

import android.os.AsyncTask;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;

/**
 * This class request the extraction of the data from track.
 */
public class TrackDetailInteractor implements
        AsyncOperation<Void, TrackDataLoader.TrackDataItem, Void, String>, Destructible {
    private static TrackDataLoader mTrackDataLoader;
    private AsyncOperation<Void, TrackDataLoader.TrackDataItem, Void, String> mLoaderListener;
    @Inject
    public TrackRoomDatabase trackRoomDatabase;

    public TrackDetailInteractor(){

    }

    public void setLoaderListener(AsyncOperation<Void, TrackDataLoader.TrackDataItem, Void, String> loader){
        mLoaderListener = loader;
    }

    /**
     * Reads asynchronously the data from track.
     * @param id The id of the track to read.
     * @return true if this task could be started, false otherwise
     */
    public boolean loadInfoTrack(int id){
        if(mTrackDataLoader == null){
            mTrackDataLoader = new TrackDataLoader();
            mTrackDataLoader.setListener(this);
            mTrackDataLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
            return true;
        }

        return false;
    }

    /**
     * Callback when the track data reading starts.
     * @param params Void params
     */
    @Override
    public void onAsyncOperationStarted(Void params) {
        if(mLoaderListener != null)
            mLoaderListener.onAsyncOperationStarted(null);
    }

    /**
     * Callback when the track data reading finishes.
     * @param result The info of track.
     */
    @Override
    public void onAsyncOperationFinished(TrackDataLoader.TrackDataItem result) {
        if(mLoaderListener != null)
            mLoaderListener.onAsyncOperationFinished(result);
        mTrackDataLoader.setListener(null);
        mTrackDataLoader = null;
    }

    /**
     * Callback when the track data reading is cancelled.
     * @param cancellation Void params.
     */
    @Override
    public void onAsyncOperationCancelled(Void cancellation) {
        if(mLoaderListener != null) {
            mLoaderListener.onAsyncOperationCancelled(null);
        }

        if(mTrackDataLoader != null) {
            mTrackDataLoader.setListener(null);
        }
        mTrackDataLoader = null;
    }

    /**
     * Callback when the track data reading has encountered an error..
     * @param error  The error message.
     */
    @Override
    public void onAsyncOperationError(String error) {
        if(mLoaderListener != null)
            mLoaderListener.onAsyncOperationError(error);

        mTrackDataLoader.setListener(null);
        mTrackDataLoader = null;
    }

    /**
     * Public method for release resources.
     */
    @Override
    public void destroy() {
        if(mTrackDataLoader != null){
            mTrackDataLoader.cancel(true);
        }
        onAsyncOperationCancelled(null);
        mTrackDataLoader = null;
    }

}
