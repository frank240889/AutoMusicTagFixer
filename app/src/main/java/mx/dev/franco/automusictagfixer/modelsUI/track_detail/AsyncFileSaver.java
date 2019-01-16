package mx.dev.franco.automusictagfixer.modelsUI.track_detail;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.webkit.MimeTypeMap;

import com.crashlytics.android.Crashlytics;

import java.io.IOException;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;

/**
 * Saves cover art as image file, using
 * another thread to avoid blocking UI
 */

public class AsyncFileSaver extends AsyncTask<Void, Void, String> {

    public interface OnSaveListener{
        void onSavingStart();
        void onSavingFinished(String newPath);
        void onSavingError(String error);
    }
    //Data of cover art to save
    private byte[] mDataImage;
    //Tags used for using in name of output file
    private String mTitle, mArtist, mAlbum;
    private String mGeneratedPath;
    private OnSaveListener mOnSaveListener;
    private String mError;
    @Inject
    public Context context;
    public AsyncFileSaver(byte[] dataImage, String... data){
        mDataImage = dataImage;
        mTitle = data[0];
        mArtist = data[1];
        mAlbum = data[2];
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    public void setOnSavingListener(OnSaveListener listener){
        mOnSaveListener = listener;
    }

    @Override
    protected void onPreExecute(){
        if(mOnSaveListener != null)
            mOnSaveListener.onSavingStart();
    }

    @Override
    protected String doInBackground(Void... voids) {
        mGeneratedPath = null;
        try {
            mGeneratedPath = mx.dev.franco.automusictagfixer.utilities.FileSaver.saveImageFile(mDataImage, mTitle, mArtist, mAlbum);
            //if was successful saved, then
            //inform to system that one file has been created
            if(mGeneratedPath != null && !mGeneratedPath.equals(mx.dev.franco.automusictagfixer.utilities.FileSaver.NULL_DATA)
                    && !mGeneratedPath.equals(mx.dev.franco.automusictagfixer.utilities.FileSaver.NO_EXTERNAL_STORAGE_WRITABLE)
                    && !mGeneratedPath.equals(mx.dev.franco.automusictagfixer.utilities.FileSaver.INPUT_OUTPUT_ERROR)) {
                MediaScannerConnection.scanFile(
                        context,
                        new String[]{mGeneratedPath},
                        new String[]{MimeTypeMap.getFileExtensionFromUrl(mGeneratedPath)},
                        null);
            }

        } catch (IOException e) {
            Crashlytics.logException(e);
            mError = e.getMessage();
            return null;
        }

        return mGeneratedPath;
    }

    @Override
    protected void onPostExecute(String path){
        if(mOnSaveListener != null){
            if(mGeneratedPath != null && !mGeneratedPath.equals(mx.dev.franco.automusictagfixer.utilities.FileSaver.NULL_DATA)
                    && !mGeneratedPath.equals(mx.dev.franco.automusictagfixer.utilities.FileSaver.NO_EXTERNAL_STORAGE_WRITABLE)
                    && !mGeneratedPath.equals(mx.dev.franco.automusictagfixer.utilities.FileSaver.INPUT_OUTPUT_ERROR)) {
                mOnSaveListener.onSavingFinished(path);
            }
            else {
                mOnSaveListener.onSavingError(mError);
            }
        }
        mOnSaveListener = null;
        context = null;
    }

}