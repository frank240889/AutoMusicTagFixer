package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import org.jaudiotagger.tag.FieldKey;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.Resource;

/**
 * This class manages the some operations to MediaStore.
 */
public class MediaStoreManager {

    private SingleLiveEvent<MediaStoreResult> mMediaStoreResultSingleLiveEvent;
    //Live data to inform the progress of task.
    private SingleLiveEvent<Boolean> mLoadingStateLiveData;
    private SingleLiveEvent<Resource<List<Track>>> mResult;
    private MediaStoreUpdater mMediaStoreUpdater;
    private Context mContext;
    private TrackDAO mTrackDAO;


    @Inject
    public MediaStoreManager(@Nonnull Context context, @NonNull TrackDAO trackDAO) {
        mContext = context;
        mTrackDAO = trackDAO;
        mLoadingStateLiveData = new SingleLiveEvent<>();
        mMediaStoreResultSingleLiveEvent = new SingleLiveEvent<>();
        mResult = new SingleLiveEvent<>();

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
     * @param data The data to set.
     * @param task The task to execute.
     * @param mediaStoreId The id of media store file.
     */
    public void updateDataTrackOnMediaStore(Map<FieldKey,Object> data, int task, int mediaStoreId) {
        mMediaStoreUpdater = new MediaStoreUpdater(new AsyncOperation<Void, MediaStoreResult, Void, Void>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(MediaStoreResult result) {
                mLoadingStateLiveData.setValue(false);
                mMediaStoreResultSingleLiveEvent.setValue(result);
            }
        }, data, task, mediaStoreId);
        mMediaStoreUpdater.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), mContext);
    }

    /**
     * Updates the data of media store file.
     * @param path The path of the file to scan by mediastore.
     */
    public void addFileToMediaStore(String path, MediaScannerConnection.OnScanCompletedListener onScanCompletedListener) {
        MediaScannerConnection.scanFile(
            mContext,
            new String[]{path},
            new String[]{MimeTypeMap.getFileExtensionFromUrl(path)},
            onScanCompletedListener);
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

    /**
     * Reescan the media store to retrieve new audio files.
     */
    public void rescan() {
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
        }, mTrackDAO);
        mediaStoreReader.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), mContext);
    }

    public void onCleared() {
        //mResult.call();
        //mLoadingStateLiveData.call();
        //mMediaStoreResultSingleLiveEvent.call();
    }

}
