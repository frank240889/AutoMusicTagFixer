package mx.dev.franco.automusictagfixer.utilities.FileReaderImpl;

import android.database.Cursor;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.list.AudioItem;

/**
 * This class request reads information of audio files from MediaStore,
 * then creates the initial DB when app
 * is used by first time; next openings of app it
 * reads the songs from this database instead of MediaStore
 */
public class AsyncFileReader extends AsyncTask<Void, AudioItem, Void> {
    public interface FileReadingOperationListener{
        void onPreExecute();
        void onProgressUpdate(AudioItem audioItem);
        void onPostExecute();
        void onCancelled();
    }

    //Are we reading from our DB or MediaStore?
    private int taskType;
    //data retrieved from DB
    private Cursor data;
    //how many audio files were added or removed
    //from your smartphone
    private int added = 0;
    private int removed = 0;
    private boolean mMediaScanCompleted = false;
    private WeakReference<MainActivity> mWeakRef;

    public AsyncFileReader(){

    }

    @Override
    protected void onPreExecute() {


    }

    @Override
    protected Void doInBackground(Void... voids) {

        return null;
    }

    @Override
    protected void onProgressUpdate(AudioItem... audioItems) {
    }

    @Override
    protected void onPostExecute(Void result) {

    }

    @Override
    public void onCancelled(){
    }

    /**
     * This method gets data of every song
     * from media store,
     * @return A cursor with data retrieved
     */
    private Cursor getDataFromDevice() {

        return null;
    }

    /**
     * Updates list by adding any new audio file
     * detected in smartphone
     */
    private void rescanAndUpdateList(){

    }

    /**
     * Updates list by removing those items
     * that have changed its path, removed
     * or deleted.
     */
    private void removeUnusedItems(){

    }


    /**
     * Creates the DB that app uses
     * to store the state of every song,
     * generally it only runs the first time
     * the app is executed, or when DB
     * could not be created at any previous app open.
     */
    private void createNewTable(){
        data = getDataFromDevice();
        if(data.moveToFirst()) {
            do {
                createAndAddAudioItem();
            }
            while (data.moveToNext());
        }
    }

    /**
     * When app DB is generated, now it reads
     * info about songs from here, not from MediaStore
     */
    private void readFromDatabase(){

    }


    /**
     * Creates the AudioItem object with the info
     * song, then calls publish progress passing
     * this object and adding to list
     */
    private void createAndAddAudioItem(){
    }
}
