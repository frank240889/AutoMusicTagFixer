package mx.dev.franco.automusictagfixer.filemanager;

import androidx.lifecycle.LiveData;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.identifier.CoverIdentificationResult;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class FileManager {

    private SingleLiveEvent<Boolean> mStateLiveData;
    private SingleLiveEvent<Resource<String>> mLiveData;
    private Cache<String, List<CoverIdentificationResult>> mCoverCache;


    private AsyncCoverSaver mAsyncCoverSaver;

    @Inject
    public FileManager(Cache<String, List<CoverIdentificationResult>> coverCache) {
        mStateLiveData = new SingleLiveEvent<>();
        mLiveData = new SingleLiveEvent<>();
        mCoverCache = coverCache;
    }

    public LiveData<Boolean> observeLoadingState() {
        return mStateLiveData;
    }

    public LiveData<Resource<String>> observeResultFileSaving() {
        return mLiveData;
    }

    public void saveFile(String idCover, String trackId, String filename) {
        mAsyncCoverSaver = new AsyncCoverSaver(new AsyncOperation<Void, String, Void, String>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(String result) {
                mStateLiveData.setValue(false);
                mLiveData.setValue(Resource.success(result));
            }

            @Override
            public void onAsyncOperationError(String error) {
                mStateLiveData.setValue(false);
                mLiveData.setValue(Resource.error(error));
            }
        }, filename, idCover, trackId, mCoverCache, null);
        mAsyncCoverSaver.executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    public void saveFile(byte[] data, String filename) {
        mAsyncCoverSaver = new AsyncCoverSaver(new AsyncOperation<Void, String, Void, String>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(String result) {
                mStateLiveData.setValue(false);
                mLiveData.setValue(Resource.success(result));
            }

            @Override
            public void onAsyncOperationError(String error) {
                mStateLiveData.setValue(false);
                mLiveData.setValue(Resource.error(error));
            }
        }, filename, null, null, mCoverCache, data);
        mAsyncCoverSaver.executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    public void onCleared() {
        //mLiveData.call();
        //mStateLiveData.call();
        mAsyncCoverSaver = null;
    }
}
