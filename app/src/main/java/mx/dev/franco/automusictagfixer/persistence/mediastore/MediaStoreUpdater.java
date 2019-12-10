package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import org.jaudiotagger.tag.FieldKey;

import java.util.Map;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;

public class MediaStoreUpdater extends AsyncTask<Context, Void, MediaStoreResult> {
    private AsyncOperation<Void, MediaStoreResult, Void, Void> mCallback;
    private Map<FieldKey, Object> mData;
    private int mTask;
    private int mMediaStoreId;
    public MediaStoreUpdater(AsyncOperation<Void, MediaStoreResult, Void, Void> callback,
                             Map<FieldKey, Object> data,
                             int task,
                             int mediaStoreId) {
        mCallback = callback;
        mData = data;
        mTask = task;
        mMediaStoreId = mediaStoreId;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(null);
    }

    @Override
    protected MediaStoreResult doInBackground(Context... contexts) {
        MediaStoreResult mediaStoreResult = new MediaStoreResult();
        mediaStoreResult.setTask(mTask);
        Log.d(this.getClass().getCanonicalName(), "doInBackground");

        if(mTask == MediaStoreResult.UPDATE_RENAMED_FILE) {
            String path = (String) mData.get(FieldKey.CUSTOM1);
            ContentValues newValuesToMediaStore = new ContentValues();
            String selection = MediaStore.Audio.Media._ID + "= ?";
            String[] selectionArgs = new String[]{mMediaStoreId + ""}; //this is the old path
            newValuesToMediaStore.put(MediaStore.MediaColumns.DATA, path);
            if (mMediaStoreId != -1) {
                contexts[0].getContentResolver().
                        update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                newValuesToMediaStore,
                                selection,
                                selectionArgs);
                newValuesToMediaStore.clear();
                mediaStoreResult.setNewPath(path);
                mediaStoreResult.setTags(mData);
            }
        }
        else {
            String title = (String) mData.get(FieldKey.TITLE);
            String artist = (String) mData.get(FieldKey.ARTIST);
            String album = (String) mData.get(FieldKey.ALBUM);

            ContentValues updatedValues = new ContentValues();
            boolean shouldUpdateMediaStore = false;
            if (title != null && !title.isEmpty()) {
                updatedValues.put(MediaStore.Audio.Media.TITLE, title);
                shouldUpdateMediaStore = true;
            }
            if (artist != null && !artist.isEmpty()) {
                updatedValues.put(MediaStore.Audio.Media.ARTIST, artist);
                shouldUpdateMediaStore = true;
            }
            if (album != null && !album.isEmpty()) {
                updatedValues.put(MediaStore.Audio.Media.ALBUM, album);
                shouldUpdateMediaStore = true;
            }

            if(shouldUpdateMediaStore){
                String select = MediaStore.Audio.Media._ID + "= ?";
                String[] selectArgs = new String[]{mMediaStoreId + ""};
                contexts[0].getContentResolver().
                        update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                updatedValues,
                                select,
                                selectArgs);
                mediaStoreResult.setUpdated(true);
                mediaStoreResult.setTags(mData);
            }
            else {
                mediaStoreResult.setUpdated(false);
            }

        }

        return mediaStoreResult;
    }

    @Override
    protected void onPostExecute(MediaStoreResult mediaStoreResult) {
        if(mCallback != null)
            mCallback.onAsyncOperationFinished(mediaStoreResult);
    }

}
