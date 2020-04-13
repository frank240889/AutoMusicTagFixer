package mx.dev.franco.automusictagfixer.filemanager;

import androidx.lifecycle.LiveData;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class FileManager {

    private SingleLiveEvent<Boolean> mStateLiveData;
    private SingleLiveEvent<Resource<String>> mResultsSavingLiveData;

    private AsyncCoverSaver mAsyncCoverSaver;
    private GnApiService mGnApiService;

    @Inject
    public FileManager(GnApiService gnApiService) {
        mStateLiveData = new SingleLiveEvent<>();
        mResultsSavingLiveData = new SingleLiveEvent<>();
        mGnApiService = gnApiService;
    }

    public LiveData<Boolean> observeLoadingState() {
        return mStateLiveData;
    }

    public LiveData<Resource<String>> observeResultFileSaving() {
        return mResultsSavingLiveData;
    }

    public void saveFile(Result result, String filename) {
        mAsyncCoverSaver = new AsyncCoverSaver(result, filename, new AsyncOperation<Void, String, Void, String>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(String result) {
                mStateLiveData.setValue(false);
                mResultsSavingLiveData.setValue(Resource.success(result));
            }

            @Override
            public void onAsyncOperationError(String error) {
                mStateLiveData.setValue(false);
                mResultsSavingLiveData.setValue(Resource.error(error));
            }
        });

        mAsyncCoverSaver.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), mGnApiService);
    }

    public void saveFile(byte[] data, String filename) {
        mAsyncCoverSaver = new AsyncCoverSaver(data, filename, new AsyncOperation<Void, String, Void, String>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(String result) {
                mStateLiveData.setValue(false);
                mResultsSavingLiveData.setValue(Resource.success(result));
            }

            @Override
            public void onAsyncOperationError(String error) {
                mStateLiveData.setValue(false);
                mResultsSavingLiveData.setValue(Resource.error(error));
            }
        });

        mAsyncCoverSaver.executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    public void onCleared() {
        mAsyncCoverSaver = null;
    }
}
