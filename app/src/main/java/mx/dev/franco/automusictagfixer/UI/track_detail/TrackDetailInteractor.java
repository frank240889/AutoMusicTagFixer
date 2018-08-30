package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.os.AsyncTask;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.media_store_retriever.AsyncFileReader;
import mx.dev.franco.automusictagfixer.room.TrackRoomDatabase;

public class TrackDetailInteractor implements TrackDataLoader.TrackLoader, AsyncFileReader.IRetriever, Destructible {
    private static TrackDataLoader mTrackDataLoader;
    private TrackDataLoader.TrackLoader mLoaderListener;
    private static AsyncFileReader sAsyncFileReader;
    private AsyncFileReader.IRetriever mUpdaterListener;
    @Inject
    TrackRoomDatabase trackRoomDatabase;

    public TrackDetailInteractor(){
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    public void setLoaderListener(TrackDataLoader.TrackLoader loader){
        mLoaderListener = loader;
    }

    public void setUpdaterListener(AsyncFileReader.IRetriever iRetriever){
        mUpdaterListener = iRetriever;
    }

    public void loadInfoTrack(int id){
        if(mTrackDataLoader == null){
            mTrackDataLoader = new TrackDataLoader();
            mTrackDataLoader.setListener(this);
            mTrackDataLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
        }
    }

    @Override
    public void onStartedLoad() {
        if(mLoaderListener != null)
            mLoaderListener.onStartedLoad();
    }

    @Override
    public void onFinishedLoad(TrackDataLoader.TrackDataItem trackDataItem) {
        if(mLoaderListener != null)
            mLoaderListener.onFinishedLoad(trackDataItem);
        mTrackDataLoader.setListener(null);
        mTrackDataLoader = null;
    }

    @Override
    public void onCancelledLoad() {
        if(mLoaderListener != null) {
            mLoaderListener.onCancelledLoad();
            mTrackDataLoader.setListener(null);
        }
        mTrackDataLoader = null;
    }

    @Override
    public void onLoadError(String error) {
        if(mLoaderListener != null)
            mLoaderListener.onLoadError(error);

        mTrackDataLoader.setListener(null);
        mTrackDataLoader = null;
    }

    @Override
    public void destroy() {
        if(mTrackDataLoader != null){
            mTrackDataLoader.cancel(true);
        }
        mTrackDataLoader = null;
        mLoaderListener = null;
    }

    @Override
    public void onStart() {
        if(mUpdaterListener != null){
            mUpdaterListener.onStart();
        }
    }

    @Override
    public void onFinish() {
        if(mUpdaterListener != null){
            mUpdaterListener.onFinish();
            sAsyncFileReader.setListener(null);
        }
        sAsyncFileReader = null;
    }

    @Override
    public void onCancel() {
        if(mUpdaterListener != null){
            mUpdaterListener.onCancel();
            sAsyncFileReader.setListener(null);
        }
        sAsyncFileReader = null;
    }
}
