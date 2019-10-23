package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jaudiotagger.tag.FieldKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.Resource;

/**
 * This class manages the some operations to MediaStore.
 */
public class MediaStoreManager {

    private SingleLiveEvent<MediaStoreResult> mMediaStoreResultSingleLiveEvent;
    //Live data to inform the progress of task.
    private MutableLiveData<Boolean> mLoadingStateLiveData;
    private MutableLiveData<Resource<List<Track>>> mResult;
    private MediaStoreUpdater mMediaStoreUpdater;
    private Context mContext;


    @Inject
    public MediaStoreManager(@Nonnull Context context) {
        mContext = context;
        mLoadingStateLiveData = new MutableLiveData<>();
        mMediaStoreResultSingleLiveEvent = new SingleLiveEvent<>();
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
    public void updateMediaStore(Map<FieldKey,Object> data, int task, int mediaStoreId) {
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
        mMediaStoreUpdater.executeOnExecutor(Executors.newCachedThreadPool(), mContext);
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
        mediaStoreReader.executeOnExecutor(Executors.newCachedThreadPool(), mContext);
    }

}