package mx.dev.franco.automusictagfixer.fixer;

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
import mx.dev.franco.automusictagfixer.identifier.GnResponseListener;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackState;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

public class Fixer extends AsyncTask<GnResponseListener.IdentificationResults,Void,Boolean> {
    public interface OnCorrectionListener {
        void onCorrectionStarted(Track track);
        void onCorrectionCompleted(Tagger.ResultCorrection resultCorrection, Track track);
        void onCorrectionCancelled(Track track);
        void onCorrectionError(Tagger.ResultCorrection resultCorrection, Track track);
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
                    result = updateCoverArt(results == null ? null : results[0]);
                } catch (ReadOnlyFileException | CannotReadException | TagException | InvalidAudioFrameException | IOException e) {
                    e.printStackTrace();
                }
                break;
            case Tagger.MODE_WRITE_ONLY_MISSING:
            case Tagger.MODE_OVERWRITE_ALL_TAGS:
                try {
                    result = updateTags(results[0], mTask);
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
                mListener.onCorrectionError(mResultsCorrection, mTrack);
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
        }
        if(results.artist != null && !results.artist.equals("")){
            tagsToApply.put(FieldKey.ARTIST, results.artist );
            dataArtist = true;
        }
        if(results.album != null && !results.album.equals("")){
            tagsToApply.put(FieldKey.ALBUM, results.album);
            dataAlbum = true;
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
        mResultsCorrection.track = mTrack;

        if(hasError(mResultsCorrection.code))
            return false;

        //Get tags that were updated to update item in internal app DB and MediaStore.
        if(mResultsCorrection.tagsUpdated != null) {
            ContentValues updatedValues = new ContentValues();
             boolean shouldUpdateMediaStore = false;
            if (mResultsCorrection.tagsUpdated.containsKey(FieldKey.TITLE)) {
                mTrack.setTitle((String) mResultsCorrection.tagsUpdated.get(FieldKey.TITLE));
                updatedValues.put(MediaStore.Audio.Media.TITLE, mTrack.getTitle());
                shouldUpdateMediaStore = true;
            }
            if (mResultsCorrection.tagsUpdated.containsKey(FieldKey.ARTIST)) {
                mTrack.setArtist((String) mResultsCorrection.tagsUpdated.get(FieldKey.ARTIST));
                updatedValues.put(MediaStore.Audio.Media.ARTIST, mTrack.getArtist());
                shouldUpdateMediaStore = true;
            }
            if (mResultsCorrection.tagsUpdated.containsKey(FieldKey.ALBUM)) {
                mTrack.setAlbum((String) mResultsCorrection.tagsUpdated.get(FieldKey.ALBUM));
                updatedValues.put(MediaStore.Audio.Media.ALBUM, mTrack.getAlbum());
                shouldUpdateMediaStore = true;
            }

            if(shouldUpdateMediaStore && mTrack.getMediaStoreId() != -1){
                String select = MediaStore.Audio.Media._ID + "= ?";
                String[] selectArgs = new String[]{mTrack.getMediaStoreId() + ""};
                boolean successMediaStore = context.getContentResolver().
                        update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                updatedValues,
                                select,
                                selectArgs) == 1;

                Log.d(TAG, "success media store update item: " + successMediaStore);

            }
        }
        //If some info was not identificationFound, mark its state as INCOMPLETE
        if (!dataTitle || !dataArtist || !dataAlbum || !dataImage || !dataTrackNumber || !dataYear || !dataGenre) {
            mResultsCorrection.allTagsApplied = TrackState.ALL_TAGS_NOT_FOUND;
            mTrack.setState(TrackState.ALL_TAGS_NOT_FOUND);
        }
        //All info for this song was identificationFound, mark its state as COMPLETE!!!
        else {
            mResultsCorrection.allTagsApplied = TrackState.ALL_TAGS_FOUND;
            mTrack.setState(TrackState.ALL_TAGS_FOUND);
        }


        String newAbsolutePath = null;
        //Rename file if this option is enabled in Settings
        if (mShouldRename) {

            //User did no provide a name, so use title, artist and album
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

            //Renamed successfully, so updat MediaStore
            if (newAbsolutePath != null) {
                ContentValues newValuesToMediaStore = new ContentValues();
                String selection = MediaStore.Audio.Media._ID + "= ?";
                String[] selectionArgs = new String[]{mTrack.getMediaStoreId() + ""}; //this is the old path
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
            }
        }
        return true;
    }

    private boolean updateCoverArt(GnResponseListener.IdentificationResults results) throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {
        if(mTask == Tagger.MODE_ADD_COVER){
            //Here we update cover
            mResultsCorrection = taggerHelper.applyCover(results.cover, mTrack.getPath());
            mResultsCorrection.track = mTrack;
            mTrack.setState(TrackState.ALL_TAGS_FOUND);
        }
        //remove cover
        else {
            mResultsCorrection = taggerHelper.applyCover(null, mTrack.getPath());
            mResultsCorrection.track = mTrack;
            mTrack.setState(TrackState.ALL_TAGS_NOT_FOUND);
        }

        if(hasError(mResultsCorrection.code))
            return false;

        return true;
    }

    private boolean hasError(int result){
        switch (result) {
            case Tagger.COULD_NOT_APPLY_COVER:
            case Tagger.COULD_NOT_APPLY_TAGS:
            case Tagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
            case Tagger.COULD_NOT_CREATE_AUDIOFILE:
            case Tagger.COULD_NOT_CREATE_TEMP_FILE:
            case Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE:
            case Tagger.COULD_NOT_READ_TAGS:
            case Tagger.COULD_NOT_REMOVE_COVER:
                return true;
        }

        return false;
    }


    public static class CorrectionParams{
        public int dataFrom;
        public int mode;
        public boolean shouldRename = false;
        public String newName = "";
    }


    public static class ERROR_CODES{
        public static String getErrorMessage(ResourceManager resourceManager, int errorCode){
            String errorMessage;
            switch (errorCode){
                case Tagger.COULD_NOT_APPLY_COVER:
                    errorMessage = resourceManager.getString(R.string.message_could_not_apply_cover);
                    break;
                case Tagger.COULD_NOT_APPLY_TAGS:
                    errorMessage = resourceManager.getString(R.string.message_could_not_apply_tags);
                    break;
                case Tagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
                    errorMessage = resourceManager.getString(R.string.message_could_copy_back);
                    break;
                case Tagger.COULD_NOT_CREATE_AUDIOFILE:
                    errorMessage = resourceManager.getString(R.string.message_could_not_create_audio_file);
                    break;
                case Tagger.COULD_NOT_CREATE_TEMP_FILE:
                    errorMessage = resourceManager.getString(R.string.message_could_not_create_temp_file);
                    break;
                case Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE:
                    errorMessage = resourceManager.getString(R.string.message_uri_tree_not_set);
                    break;
                case Tagger.COULD_NOT_READ_TAGS:
                    errorMessage = resourceManager.getString(R.string.message_could_not_read_tags);
                    break;
                case Tagger.COULD_NOT_REMOVE_COVER:
                    errorMessage = resourceManager.getString(R.string.message_could_not_remove_cover);
                    break;
                default:
                    errorMessage = resourceManager.getString(R.string.error);
                    break;
            }

            return errorMessage;
        }

        public static String getErrorMessage(Context context, int errorCode){
            String errorMessage;
            switch (errorCode){
                case Tagger.COULD_NOT_APPLY_COVER:
                    errorMessage = context.getString(R.string.message_could_not_apply_cover);
                    break;
                case Tagger.COULD_NOT_APPLY_TAGS:
                    errorMessage = context.getString(R.string.message_could_not_apply_tags);
                    break;
                case Tagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
                    errorMessage = context.getString(R.string.message_could_copy_back);
                    break;
                case Tagger.COULD_NOT_CREATE_AUDIOFILE:
                    errorMessage = context.getString(R.string.message_could_not_create_audio_file);
                    break;
                case Tagger.COULD_NOT_CREATE_TEMP_FILE:
                    errorMessage = context.getString(R.string.message_could_not_create_temp_file);
                    break;
                case Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE:
                    errorMessage = context.getString(R.string.message_uri_tree_not_set);
                    break;
                case Tagger.COULD_NOT_READ_TAGS:
                    errorMessage = context.getString(R.string.message_could_not_read_tags);
                    break;
                case Tagger.COULD_NOT_REMOVE_COVER:
                    errorMessage = context.getString(R.string.message_could_not_remove_cover);
                    break;
                default:
                    errorMessage = context.getString(R.string.error);
                    break;
            }

            return errorMessage;
        }
    }
}
