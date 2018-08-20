package mx.dev.franco.automusictagfixer.services.Fixer;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

public class Fixer extends AsyncTask<GnResponseListener.IdentificationResults,Void,Boolean> {
    public interface OnCorrectionListener {
        void onCorrectionStarted(Track track);
        void onCorrectionCompleted(Tagger.ResultCorrection resultCorrection, Track track);
        void onCorrectionCancelled(Track track);
        void onCorrectionError(Track track, String error);
    }

    private static final String TAG = Fixer.class.getName();
    private Track mTrack;
    private OnCorrectionListener mListener;
    private boolean mShouldRename = true;
    private String mRenameTo = "";
    @Inject
    Tagger taggerHelper;
    @Inject
    TrackRepository trackRepository;
    @Inject
    ResourceManager resourceManager;
    @Inject
    Context context;

    private Tagger.ResultCorrection mResultsCorrection;
    private int mTask;

    public Fixer(OnCorrectionListener listener){
        mListener = listener;
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    public void setTrack(Track track){
        mTrack = track;
    }

    public void setTask(int task){
        mTask = task;
    }

    public void setShouldRename(boolean shouldRename){
        mShouldRename = shouldRename;
    }

    public void renameTo(String newName){
        mRenameTo = newName;
    }

    @Override
    protected void onPreExecute(){
        if(mListener != null){
            mListener.onCorrectionStarted(mTrack);
        }
    }

    @Override
    protected final Boolean doInBackground(GnResponseListener.IdentificationResults... results) {
        boolean result = false;
        switch (mTask){
            case Tagger.MODE_ADD_COVER:
            case Tagger.MODE_REMOVE_COVER:
                try {
                    updateCoverArt(results == null ? null : results[0]);
                } catch (ReadOnlyFileException | CannotReadException | TagException | InvalidAudioFrameException | IOException e) {
                    e.printStackTrace();
                }
                break;
            case Tagger.MODE_WRITE_ONLY_MISSING:
            case Tagger.MODE_OVERWRITE_ALL_TAGS:
                try {
                    updateTags(results[0], mTask);
                } catch (TagException | ReadOnlyFileException | CannotReadException | IOException | InvalidAudioFrameException e) {
                    e.printStackTrace();
                }
                break;
        }


        return result;
    }

    @Override
    protected void onPostExecute(Boolean booleans){
        if(mListener != null){
            if(booleans)
                mListener.onCorrectionCompleted(mResultsCorrection, mTrack);
            else
                mListener.onCorrectionError(
                        mTrack,
                        String.format(resourceManager.getString(R.string.file_status_in_sd_without_permission), AudioItem.getPath(mTrack.getPath())));
        }
        clear();
    }

    @Override
    public void onCancelled(){
        clear();
    }
    public void onCancelled(Boolean booleans){
        if(mListener != null)
            mListener.onCorrectionCancelled(mTrack);

        super.onCancelled(booleans);
        clear();
    }

    private void clear(){
        if(taggerHelper != null) {
            taggerHelper = null;
        }
        mTrack = null;
        trackRepository = null;
        context = null;
    }

    private boolean updateTags(GnResponseListener.IdentificationResults results, int overwriteTags) throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException {
        if(isCancelled())
            return true;
        //check what data has been identificationFound
        boolean dataTitle = false;
        boolean dataArtist = false;
        boolean dataAlbum = false;
        boolean dataImage = false;
        boolean dataTrackNumber = false;
        boolean dataYear = false;
        boolean dataGenre = false;

        if(isCancelled())
            return true;

        HashMap<FieldKey, Object> tagsToApply = new HashMap<>();

        if(isCancelled())
            return true;

        if(results.title != null && !results.title.equals("")){
            tagsToApply.put(FieldKey.TITLE, results.title);
            dataTitle = true;
            mTrack.setTitle(results.title);
        }
        if(results.artist != null && !results.artist.equals("")){
            tagsToApply.put(FieldKey.ARTIST, results.artist );
            dataArtist = true;
            mTrack.setArtist(results.artist);
        }
        if(results.album != null && !results.album.equals("")){
            tagsToApply.put(FieldKey.ALBUM, results.album);
            dataAlbum = true;
            mTrack.setAlbum(results.album);
        }
        if(results.cover != null){
            tagsToApply.put(FieldKey.COVER_ART, results.cover);
            dataImage = true;
        }
        if(results.trackNumber != null && !results.trackNumber.equals("")){
            tagsToApply.put(FieldKey.TRACK, results.trackNumber);
            dataTrackNumber = true;
        }
        if(results.trackYear != null && !results.trackYear.equals("")){
            tagsToApply.put(FieldKey.YEAR, results.trackYear);
            dataYear = true;
        }
        if(results.genre != null && !results.genre.equals("")){
            tagsToApply.put(FieldKey.GENRE, results.genre);
            dataGenre = true;
        }

        if(isCancelled())
            return true;

        mResultsCorrection = taggerHelper.saveTags(mTrack.getPath(), tagsToApply, overwriteTags);

        if(mResultsCorrection.code == Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE)
            return false;

        mResultsCorrection.track = mTrack;
        //If some info was not identificationFound, mark its state as INCOMPLETE
        if (!dataTitle || !dataArtist || !dataAlbum || !dataImage || !dataTrackNumber || !dataYear || !dataGenre) {
            mResultsCorrection.allTagsApplied = AudioItem.STATUS_ALL_TAGS_NOT_FOUND;
            mTrack.setState(AudioItem.STATUS_ALL_TAGS_NOT_FOUND);
        }
        //All info for this song was identificationFound, mark its state as COMPLETE!!!
        else {
            mResultsCorrection.allTagsApplied = AudioItem.STATUS_ALL_TAGS_FOUND;
            mTrack.setState(AudioItem.STATUS_ALL_TAGS_FOUND);
        }

        String selection = null;
        String[] selectionArgs = null; //this is the old path
        String newAbsolutePath = null;
        //Rename file if this option is enabled in Settings
        if (mShouldRename) {

            if (mRenameTo.isEmpty()) {

                newAbsolutePath = taggerHelper.renameFile(new File(mTrack.getPath()),
                        results.title,
                        results.artist,
                        results.album);
            } else {
                newAbsolutePath = taggerHelper.renameFile(new File(mTrack.getPath()),
                        mRenameTo,
                        results.artist,
                        results.album);
            }

            if (newAbsolutePath != null) {
                ContentValues newValuesToMediaStore = new ContentValues();
                selection = MediaStore.Audio.Media._ID + "= ?";
                selectionArgs = new String[]{mTrack.getMediaStoreId() + ""}; //this is the old path
                mResultsCorrection.pathTofileUpdated = newAbsolutePath;
                newValuesToMediaStore.put(MediaStore.MediaColumns.DATA, newAbsolutePath);
                if (mTrack.getMediaStoreId() != -1) {
                    boolean successMediaStore = context.getContentResolver().
                            update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    newValuesToMediaStore,
                                    selection,
                                    selectionArgs) == 1;
                    newValuesToMediaStore.clear();
                    Log.d(TAG, "success media store update: " + successMediaStore);
                }
                mResultsCorrection.track.setPath(newAbsolutePath);
            /*MediaScannerConnection.scanFile(context, new String[]{newAbsolutePath}, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Log.d(TAG,"success media store scnned: " + path);
                }
            });*/

            }
        }
        return true;
    }

    private boolean updateCoverArt(GnResponseListener.IdentificationResults results) throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {
        if(mTask == Tagger.MODE_ADD_COVER){
            //Here we update cover
            mResultsCorrection = taggerHelper.applyCover(results.cover, mTrack.getPath());
        }
        //remove cover
        else {
            mResultsCorrection = taggerHelper.applyCover(null, mTrack.getPath());
        }
        mResultsCorrection.track = mTrack;

        if(mResultsCorrection.code == Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE)
            return false;

        return true;
    }


    public static class CorrectionParams{
        public int dataFrom;
        public int mode;
        public boolean shouldRename = false;
        public String newName = "";
    }
}
