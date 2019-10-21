package mx.dev.franco.automusictagfixer.filemanager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executors;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class FileManager {

    private MutableLiveData<Boolean> mStateLiveData;
    private SingleLiveEvent<Resource<String>> mLiveData;


    private AsyncFileSaver mAsyncFileSaver;

    public FileManager() {
        mStateLiveData = new MutableLiveData<>();
        mLiveData = new SingleLiveEvent<>();
    }

    public LiveData<Boolean> observeLoadingState() {
        return mStateLiveData;
    }

    public LiveData<Resource<String>> observeResultFileSaving() {
        return mLiveData;
    }


    public void saveFile(byte[] data, String filename) {
        mAsyncFileSaver = new AsyncFileSaver(new AsyncOperation<Void, String, Void, String>() {
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
        }, data, filename);
        mAsyncFileSaver.executeOnExecutor(Executors.newCachedThreadPool());
    }
}
