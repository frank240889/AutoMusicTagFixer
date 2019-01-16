package mx.dev.franco.automusictagfixer.modelsUI.track_detail;

import android.os.AsyncTask;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;

public class TrackDetailInteractor implements
        AsyncOperation<Void, TrackDataLoader.TrackDataItem, Void, String>, Destructible {
    private static TrackDataLoader mTrackDataLoader;
    private AsyncOperation<Void, TrackDataLoader.TrackDataItem, Void, String> mLoaderListener;
    @Inject
    public TrackRoomDatabase trackRoomDatabase;

    public TrackDetailInteractor(){
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    public void setLoaderListener(AsyncOperation<Void, TrackDataLoader.TrackDataItem, Void, String> loader){
        mLoaderListener = loader;
    }

    public void loadInfoTrack(int id){
        if(mTrackDataLoader == null){
            mTrackDataLoader = new TrackDataLoader();
            mTrackDataLoader.setListener(this);
            mTrackDataLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
        }
    }

    @Override
    public void onAsyncOperationStarted(Void params) {
        if(mLoaderListener != null)
            mLoaderListener.onAsyncOperationStarted(null);
    }

    @Override
    public void onAsyncOperationFinished(TrackDataLoader.TrackDataItem result) {
        if(mLoaderListener != null)
            mLoaderListener.onAsyncOperationFinished(result);
        mTrackDataLoader.setListener(null);
        mTrackDataLoader = null;
    }

    @Override
    public void onAsyncOperationCancelled(Void cancellation) {
        if(mLoaderListener != null) {
            mLoaderListener.onAsyncOperationCancelled(null);
            mTrackDataLoader.setListener(null);
        }
        mTrackDataLoader = null;
    }

    @Override
    public void onAsyncOperationError(String error) {
        if(mLoaderListener != null)
            mLoaderListener.onAsyncOperationError(error);

        mTrackDataLoader.setListener(null);
        mTrackDataLoader = null;
    }

    @Override
    public void destroy() {
        if(mTrackDataLoader != null){
            mTrackDataLoader.cancel(true);
        }
        onAsyncOperationCancelled(null);
        mTrackDataLoader = null;
    }

}
