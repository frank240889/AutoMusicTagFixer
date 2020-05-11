package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.Resource;

/**
 * This class manages some operations to MediaStore.
 */
public class MediaStoreManager {

    private SingleLiveEvent<MediaStoreResult> mMediaStoreResultSingleLiveEvent;
    //Live data to inform the progress of task.
    private SingleLiveEvent<Boolean> mLoadingStateLiveData;
    private SingleLiveEvent<Resource<List<Track>>> mResult;
    private MediaStoreUpdater mMediaStoreUpdater;
    private Context mContext;
    private TrackDAO mTrackDAO;
    private ContentObserver mDataSetObserver;
    private Cursor mDataset;


    @Inject
    public MediaStoreManager(@Nonnull Context context, @NonNull TrackDAO trackDAO) {
        mContext = context;
        mTrackDAO = trackDAO;
        mLoadingStateLiveData = new SingleLiveEvent<>();
        mMediaStoreResultSingleLiveEvent = new SingleLiveEvent<>();
        mResult = new SingleLiveEvent<>();
        mDataSetObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                fetchAudioFiles();
            }
        };

    }

    public LiveData<Boolean> observeLoadingState() {
        return mLoadingStateLiveData;
    }

    public LiveData<Resource<List<Track>>> observeResult() {
        return mResult;
    }

    public LiveData<MediaStoreResult> observeMediaStoreResult() {
        return mMediaStoreResultSingleLiveEvent;
    }

    /**
     * Updates the data of media store file.
     * @param path The path of the file to scan by mediastore.
     */
    public void addFileToMediaStore(String path, MediaScannerConnection.OnScanCompletedListener onScanCompletedListener) {
        MediaStoreHelper.addFileToMediaStore(path, mContext, onScanCompletedListener);
    }

    public void registerMediaContentObserver() {
        //Select all music
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        //Columns to retrieve
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.AlbumColumns.ALBUM ,
                MediaStore.Audio.Media.DATA // absolute path to audio file
        };

        mDataset = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);
        mDataset.registerContentObserver(mDataSetObserver);
    }

    public void unregisterMediaContentObserver() {
        mDataset.unregisterContentObserver(mDataSetObserver);
    }

    /**
     * Fetch files from media store.
     */
    public void fetchAudioFiles() {
        MediaStoreReader mediaStoreReader = new MediaStoreReader(new AsyncOperation<Void, List<Track>, Void, Void>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(List<Track> result) {
                mLoadingStateLiveData.setValue(false);
                mResult.setValue(Resource.success(result));
            }

            @Override
            public void onAsyncOperationError(Void error) {
                mLoadingStateLiveData.setValue(false);
                mResult.setValue(Resource.error(null));
            }
        });
        mediaStoreReader.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), mContext);
    }

}
