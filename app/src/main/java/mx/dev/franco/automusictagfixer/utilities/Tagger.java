package mx.dev.franco.automusictagfixer.utilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.images.AndroidArtwork;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mx.dev.franco.automusictagfixer.UI.track_detail.TrackDataLoader;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.room.Track;

import static mx.dev.franco.automusictagfixer.list.AudioItem.getExtension;

public class Tagger {
    private static final String TAG = Tagger.class.getName();
    //Constants to overwrite all tags or only apply those missing
    public static final int MODE_OVERWRITE_ALL_TAGS = 0;
    public static final int MODE_WRITE_ONLY_MISSING = 1;
    public static final int MODE_REMOVE_COVER = 2;
    public static final int MODE_ADD_COVER = 3;

    //Constants that indicates reason why cannot perform the operation
    public static final int COULD_NOT_CREATE_AUDIOFILE = 10;
    public static final int COULD_NOT_CREATE_TEMP_FILE = 11;
    public static final int COULD_NOT_READ_TAGS = 12;
    public static final int COULD_NOT_GET_URI_SD_ROOT_TREE = 13;
    public static final int COULD_NOT_APPLY_TAGS = 14;
    public static final int COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION = 15;
    public static final int COULD_NOT_APPLY_COVER = 16;
    public static final int COULD_NOT_REMOVE_COVER = 17;
    public static final int COULD_NOT_REMOVE_OLD_ID3_VERSION = 18;
    private static final int SUCCESS_COPY_BACK = 19;

    //Constants to indicate a successful operation
    public static final int CURRENT_COVER_REMOVED = 20;
    public static final int NEW_COVER_APPLIED = 21;
    public static final int APPLIED_ONLY_MISSING_TAGS = 22;
    public static final int APPLIED_ALL_TAGS = 23;

    //Constants to indicate that operation was successful
    //but previous and new tags were the same.
    public static final int APPLIED_SAME_TAGS = 24;
    public static final int APPLIED_SAME_COVER = 25;

    //No identificationError is set
    public static final int NOT_SET = -1;

    private static Tagger sTaggerHelper;
    private static Context sContext;
    private static final int BUFFER_SIZE = 131072;//->128Kb

    private static StorageHelper sStorageHelper;

    /**
     * A single instance of this helper
     * @param context
     */
    private Tagger(Context context, StorageHelper storageHelper) {
        sContext = context.getApplicationContext();
        TagOptionSingleton.getInstance().setAndroid(true);
        //Save genres as text, not as numeric codes
        TagOptionSingleton.getInstance().setWriteMp3GenresAsText(true);
        TagOptionSingleton.getInstance().setWriteMp4GenresAsText(true);
        sStorageHelper = storageHelper;
    }

    /**
     * Creates a singleton of this class
     * @param context The app context needed to create this singleton
     * @return Tagger singleton
     */
    public static synchronized Tagger getInstance(Context context, StorageHelper storageHelper) {
        if (sTaggerHelper == null) {
            sTaggerHelper = new Tagger(context, storageHelper);
        }

        return sTaggerHelper;
    }

    public ResultCorrection saveTags(String pathToFile, HashMap<FieldKey, Object> tags, int overWriteTags) throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {

        if(pathToFile == null)
            throw new NullPointerException("Path to file has not been set yet.");

        return saveTags(new File(pathToFile), tags, overWriteTags);
    }

    public ResultCorrection saveTags(File file, HashMap<FieldKey, Object> tags, int overWriteTags) throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {

        if(file == null)
            throw new NullPointerException("File has not been set yet.");

        return applyTags(file, tags, overWriteTags);
    }

    public TrackDataLoader.TrackDataItem readFile(File file) throws IOException, TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException {
        if(file == null)
            throw new NullPointerException("Source file has not been set yet.");
        if(!file.exists())
            throw new FileNotFoundException("File does not exist");

        //AudioFile handled by jAudioTagger library
        AudioFile audioFile = AudioFileIO.read(file);

        return getData(audioFile);
    }


    private TrackDataLoader.TrackDataItem getData(AudioFile audioFile){
        TrackDataLoader.TrackDataItem trackDataItem = new TrackDataLoader.TrackDataItem();

        File file = audioFile.getFile();
        String extension = AudioItem.getExtension(file);
        String mimeType = AudioItem.getMimeType(file);

        trackDataItem.extension = extension;
        trackDataItem.fileName = file.getName();
        trackDataItem.path = file.getParent();

        Tag tag = getTag(audioFile);
        AudioHeader audioHeader = audioFile.getAudioHeader();
        //Get header info and current tags
        trackDataItem.duration = AudioItem.getHumanReadableDuration(audioHeader.getTrackLength() + "");
        trackDataItem.bitrate = AudioItem.getBitrate(audioHeader.getBitRate());
        trackDataItem.frequency = AudioItem.getFrequency(audioHeader.getSampleRate());
        trackDataItem.resolution = AudioItem.getResolution(audioHeader.getBitsPerSample());
        trackDataItem.channels = audioHeader.getChannels();
        trackDataItem.fileType = audioHeader.getFormat();
        trackDataItem.fileSize = AudioItem.getFileSize(file.length());

        trackDataItem.title = tag.getFirst(FieldKey.TITLE);
        trackDataItem.artist = tag.getFirst(FieldKey.ARTIST);
        trackDataItem.album = tag.getFirst(FieldKey.ALBUM);
        trackDataItem.trackNumber = tag.getFirst(FieldKey.TRACK);
        trackDataItem.trackYear = tag.getFirst(FieldKey.YEAR);
        trackDataItem.genre = tag.getFirst(FieldKey.GENRE);

        trackDataItem.cover = (tag.getFirstArtwork() != null && tag.getFirstArtwork().getBinaryData() != null) ?
                tag.getFirstArtwork().getBinaryData() :
                null;
        trackDataItem.imageSize = AudioItem.getStringImageSize(trackDataItem.cover);

        return trackDataItem;
    }

    public TrackDataLoader.TrackDataItem readFile(String path) throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {
        if(path == null)
            throw new NullPointerException("Path to file has not been set yet.");
        return readFile(new File(path));
    }

    private ResultCorrection applyTags(File file, HashMap<FieldKey, Object> tags, int overWriteTags) throws ReadOnlyFileException, IOException, TagException, InvalidAudioFrameException, CannotReadException {
        boolean isStoredInSd = sStorageHelper.isStoredInSD(file);
        ResultCorrection resultCorrection = new ResultCorrection();

        if(isStoredInSd){
            if(AndroidUtils.getUriSD(sContext) == null) {
                resultCorrection.code = COULD_NOT_GET_URI_SD_ROOT_TREE;
            }
            else {
                HashMap<FieldKey, Object> tagsToUpdate = isNeededUpdateTags(overWriteTags,file, tags);
                if(tagsToUpdate.isEmpty()){
                    resultCorrection.code = APPLIED_SAME_TAGS;
                }
                else {
                    resultCorrection.tagsUpdated = tagsToUpdate;
                    resultCorrection = applyTagsForDocumentFileObject(file, tagsToUpdate, overWriteTags);
                }
            }
        }
        else {
            HashMap<FieldKey, Object> tagsToUpdate = isNeededUpdateTags(overWriteTags,file, tags);
            if(tagsToUpdate.isEmpty()){
                resultCorrection.code = APPLIED_SAME_TAGS;
            }
            else {
                resultCorrection.tagsUpdated = tagsToUpdate;
                resultCorrection = applyTagsForFileObject(file, tagsToUpdate, overWriteTags);
            }
        }

        return resultCorrection;
    }

    private HashMap<FieldKey, Object> isNeededUpdateTags(int mOverrideAllTags, File file, HashMap<FieldKey, Object> newTags) throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException {
        AudioFile audioFile = AudioFileIO.read(file);
        Tag currentTags = getTag(audioFile);

        byte[] currentCover = (currentTags.getFirstArtwork() != null && currentTags.getFirstArtwork().getBinaryData() != null) ?
                currentTags.getFirstArtwork().getBinaryData() :
                null;

        HashMap<FieldKey, Object> tagsToUpdate = new HashMap<>();
        //Iterates over new values tag passed, to compare
        //against the values of current tag and update only those
        //that are different than current
        for(Map.Entry entry : newTags.entrySet()){
            //For case of field cover, we need to compare the length of byte array
            if(entry.getKey() == FieldKey.COVER_ART){
                //Write missing tags only
                if(mOverrideAllTags == MODE_WRITE_ONLY_MISSING) {
                    if ((currentCover == null) || currentCover.length == 0) {
                        tagsToUpdate.put((FieldKey) entry.getKey(), entry.getValue());
                    }
                }
                //Overwrite tags, but last comparision is to check if new cover is same
                //than current, if is the same we don't update the field
                else if ((currentCover == null) || currentCover.length == 0 || currentCover.length != ((byte[])entry.getValue()).length ) {
                    tagsToUpdate.put((FieldKey) entry.getKey(), entry.getValue());
                }
            }

            //Compare for other fields if current value tag exist, and if current value tags is different that new
            else {
                if(mOverrideAllTags == MODE_WRITE_ONLY_MISSING){
                    if ( ( currentTags.getFirst((FieldKey) entry.getKey()) == null ) ||
                            currentTags.getFirst((FieldKey) entry.getKey()).isEmpty() ){
                        tagsToUpdate.put((FieldKey) entry.getKey(), entry.getValue());
                    }
                }
                else if( ( currentTags.getFirst((FieldKey) entry.getKey()) == null ) ||
                            !currentTags.getFirst((FieldKey) entry.getKey()).equals(entry.getValue())){
                        tagsToUpdate.put((FieldKey) entry.getKey(), entry.getValue());
                }

            }
        }

        return tagsToUpdate;
    }

    private Tag getTag(AudioFile audioFile){

        Tag tag = null;
        String mimeType = AudioItem.getMimeType(audioFile.getFile());
        String extension = AudioItem.getExtension(audioFile.getFile());

        if((mimeType.equals("audio/mpeg_mp3") || mimeType.equals("audio/mpeg") ) && extension.toLowerCase().equals("mp3")){
            if(((MP3File)audioFile).hasID3v1Tag() && !((MP3File) audioFile).hasID3v2Tag()){
                //create new version of ID3v2
                ID3v24Tag id3v24Tag = new ID3v24Tag( ((MP3File)audioFile).getID3v1Tag() );
                audioFile.setTag(id3v24Tag);
                tag = ((MP3File) audioFile).getID3v2TagAsv24();
            }
            else {
                //read existent V2 tag
                if(((MP3File) audioFile).hasID3v2Tag()) {
                    tag = ((MP3File) audioFile).getID3v2Tag();
                }
                //Has no tags? create a new one, but don't save until
                //user apply changes
                else {
                    ID3v24Tag id3v24Tag = new ID3v24Tag();
                    ((MP3File) audioFile).setID3v2Tag(id3v24Tag);
                    tag = ((MP3File) audioFile).getID3v2Tag();
                }
            }

        }
        else {
            //read tags or create a new one
            tag = audioFile.getTag() == null ? audioFile.createDefaultTag() : audioFile.getTag();
        }
        return tag;
    }

    /**
     * Apply tags for file stored in SD Card.
     * Is necessary create a temp file in internal storage
     * because since Android 5, user can write to SD card files
     * but only through the SAF framework that works with
     * DocumentFile objects, and JAudioTagger library
     * only works with File objects.
     * @return true if tags were successful applied, false otherwise
     */
    private ResultCorrection applyTagsForDocumentFileObject(File file, HashMap<FieldKey, Object> tagsToApply, int overwriteTags) {
        ResultCorrection resultCorrection = new ResultCorrection();
        //Creates a temp file in non removable storage to work with it
        File tempFile = sStorageHelper.createTempFileFrom(file);

        if (tempFile == null) {
            resultCorrection.code = COULD_NOT_CREATE_TEMP_FILE;
            return resultCorrection;
        }

        //Try to create AudioFile
        AudioFile audioFile = getAudioTaggerFile(tempFile);

        if (audioFile == null) {
            resultCorrection.code = COULD_NOT_CREATE_AUDIOFILE;
            return resultCorrection;
        }

        //Set new tags and get ready to apply
        setNewTags(audioFile, tagsToApply , overwriteTags);

        //Apply tags to current temp file
        try {
            audioFile.commit();
        } catch (CannotWriteException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
            resultCorrection.code = COULD_NOT_APPLY_TAGS;
            return resultCorrection;
        }

        //Try to copy temp file with its news tags to its original location
        int resultOfCopy = copyBack(tempFile, file);

        if (resultOfCopy != SUCCESS_COPY_BACK) {
            resultCorrection.code = resultOfCopy;
            return resultCorrection;
        }

        if(overwriteTags == MODE_OVERWRITE_ALL_TAGS)
            resultCorrection.code = APPLIED_ALL_TAGS;
        else
            resultCorrection.code = APPLIED_ONLY_MISSING_TAGS;

        //Delete temp file from internal storage
        deleteTempFile(tempFile);

        return resultCorrection;
    }

    /**
     * Apply tags for file stored in non removable external storage
     * (better known as internal storage).
     * @return true if successful, false otherwise
     */
    private ResultCorrection applyTagsForFileObject(File file, HashMap<FieldKey, Object> tagsToApply, int overwriteTags){
        ResultCorrection resultCorrection = new ResultCorrection();
        //Try to create AudioFile
        AudioFile audioFile = getAudioTaggerFile(file);

        if (audioFile == null) {
            resultCorrection.code = COULD_NOT_CREATE_AUDIOFILE;
            return resultCorrection;
        }

        //Put new values in its fields
        setNewTags(audioFile, tagsToApply, overwriteTags);

        try {
            audioFile.commit();
        }
        catch (CannotWriteException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            resultCorrection.code = COULD_NOT_APPLY_TAGS;
            return resultCorrection;
        }

        if(overwriteTags == MODE_OVERWRITE_ALL_TAGS)
            resultCorrection.code = APPLIED_ALL_TAGS;
        else
            resultCorrection.code = APPLIED_ONLY_MISSING_TAGS;

        return resultCorrection;
    }

    /**
     * Create an AudioFile object to to work with it
     * @return Audiofile object from temp file
     */
    private AudioFile getAudioTaggerFile(File file){
        AudioFile audioTaggerFile = null;
        try {
            audioTaggerFile = AudioFileIO.read(file);
        }
        catch (IOException | CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }

        return audioTaggerFile;
    }

    /**
     * Put new tag values into current tag of current audio file
     * @param audioFile
     */
    private void setNewTags(AudioFile audioFile,HashMap<FieldKey, Object> tagsToApply, int overwriteAllTags){
        Tag currentTag = getTag(audioFile);
        String mimeType = AudioItem.getMimeType(audioFile.getFile());
        String extension = AudioItem.getExtension(audioFile.getFile());
        boolean isMp3 = ((mimeType.equals("audio/mpeg_mp3") || mimeType.equals("audio/mpeg") ) && extension.toLowerCase().equals("mp3"));
        //remove old version of ID3 tags
        if (isMp3 && ((MP3File) audioFile).hasID3v1Tag()) {
            //Log.d("removed ID3v1","remove ID3v1");
            try {
                ((MP3File) audioFile).delete(((MP3File) audioFile).getID3v1Tag());
            } catch (IOException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
        }
        //WRITE ONLY MISSING TAGS
        if(overwriteAllTags == MODE_WRITE_ONLY_MISSING){
            for(Map.Entry entry : tagsToApply.entrySet()){
                //check the case for cover art
                if(entry.getKey() == FieldKey.COVER_ART) {

                    if (currentTag.getFirstArtwork() == null ||
                            currentTag.getFirstArtwork().getBinaryData() == null ||
                            currentTag.getFirstArtwork().getBinaryData().length == 0) {

                        currentTag.deleteArtworkField();
                        Artwork artwork = new AndroidArtwork();
                        try {
                            artwork.setBinaryData((byte[]) entry.getValue());
                            currentTag.setField(artwork);

                        } catch (FieldDataInvalidException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //check if current field from tag is null or empty
                //in that case set the field with the value passed in the mNewTags HashMap
                else {
                    try {
                        if( currentTag.getFirst((FieldKey) entry.getKey()) == null || currentTag.getFirst((FieldKey) entry.getKey()).isEmpty()  ) {

                            currentTag.setField((FieldKey) entry.getKey(), (String) entry.getValue());
                        }
                    } catch (FieldDataInvalidException e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                }

            }
        }

        //OVERWRITE ALL TAGS
        else {
            for(Map.Entry entry : tagsToApply.entrySet()){
                if(entry.getKey() == FieldKey.COVER_ART){
                    //check the case for cover art
                    Artwork artwork = new AndroidArtwork();
                    artwork.setBinaryData((byte[])entry.getValue());
                    try {
                        currentTag.deleteArtworkField();
                        currentTag.setField(artwork);
                    } catch (FieldDataInvalidException e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                }
                else {
                    try {
                        currentTag.setField((FieldKey) entry.getKey(), (String) entry.getValue());

                    } catch (FieldDataInvalidException e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                }

            }
        }

    }

    /**
     * Copy temp file to its original location in SD card
     * @return true if was correctly copied the file to its original
     *                  location, false otherwise
     * @param correctedFile
     * @param originalFile
     */
    private int copyBack(File correctedFile, File originalFile) {
        DocumentFile sourceDocumentFile = getDocumentFile(originalFile);
        if(sourceDocumentFile == null)
            return COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION;
        //Check if current file already exist and delete it
        //to replace it by the corrected file
        boolean exist = sourceDocumentFile.exists();
        boolean success = false;
        if(exist){
            boolean deleted = sourceDocumentFile.delete();
        }

        InputStream in = null;
        OutputStream out = null;
        int successCopy;
        //Copy file with new tags to its original location
        try {
            //First create a DocumentFile object referencing to its original path that it was stored
            DocumentFile newFile =  sourceDocumentFile.getParentFile().createFile(AudioItem.getMimeType(correctedFile), correctedFile.getName() );
            //Destination data
            out = sContext.getContentResolver().openOutputStream(newFile.getUri());
            //Input data
            in = new FileInputStream(correctedFile);

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            // write the output file
            out.flush();
            out.close();
            successCopy = SUCCESS_COPY_BACK;

        } catch (Exception e) {
            e.getMessage();
            Crashlytics.logException(e);
            successCopy = COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION;

        }

        return successCopy;
    }

    /**
     * Gets document file corresponding to source file
     * @return DocumentFIle of source file stored in SD location
     * @param sourceFile The file to generate its DocumentFile object
     */
    private DocumentFile getDocumentFile(File sourceFile){
        DocumentFile sourceDocumentFile = getUriTree();
        if(sourceDocumentFile == null){
            return null;
        }

        String relativePath  = getRelativePath(sourceFile);

        //Get relative path to file and get every part of relative path
        String[] parts = relativePath.split("/");
        //traverse the tree searching every folder of path from current mSourceFile
        for (String part : parts) {
            DocumentFile nextDocument = sourceDocumentFile.findFile(part);
            if (nextDocument != null) {
                sourceDocumentFile = nextDocument;
            }
        }
        Log.d("document uri",sourceDocumentFile.getUri().toString());
        //this file is not a file so we can not apply tags
        if(!sourceDocumentFile.isFile())
            return null;

        return sourceDocumentFile;
    }

    /**
     *  Gets the document file object corresponding to
     *  SD card
     * @return SD card document file
     */
    private DocumentFile getUriTree(){
        Uri uri = AndroidUtils.getUriSD(sContext);
        if(uri == null) {
            return null;
        }
        return DocumentFile.fromTreeUri(sContext, uri);
    }

    /**
     * Returns relative path of file
     * @param file The file to process
     * @return a string with the relative path
     *                  of this file
     */
    private String getRelativePath(File file){
        String baseFolder = getExtSdCardFolder(file);
        Log.d("base folder",baseFolder);

        if (baseFolder == null) {
            return null;
        }

        String relativePath;
        try {
            String fullPath = file.getCanonicalPath();
            Log.d("fullpath",fullPath);
            relativePath = fullPath.substring(baseFolder.length() + 1);
            Log.d("relativepath",relativePath);
        } catch (IOException e) {
            Crashlytics.logException(e);
            return null;
        }

        return relativePath;
    }

    /**
     * Returns the relative path of
     * storage device in which is
     * located this file
     * @param file The file to process
     * @return a string of the relative path of its
     *                  device in which is stored
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private String getExtSdCardFolder(final File file) {
        String[] extSdPaths = getExtSdCardPaths();
        try {
            for (String extSdPath : extSdPaths) {
                Log.d("starts with",file.getCanonicalPath() + "-" + extSdPath);
                //Check where is located this file, in removable or non removable
                if (file.getCanonicalPath().startsWith(extSdPath)) {

                    return extSdPath;
                }
            }
        } catch (IOException e) {
            Crashlytics.logException(e);
            return null;
        }
        return null;
    }

    /**
     * Returns an array of strings of
     * folders app, from non removable storage
     * and removable storage
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String[] getExtSdCardPaths() {
        List<String> paths = new ArrayList<>();
        //Those folders are  created  if not exist
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(sContext,"temp_tagged_files");
        if (externalFilesDirs.length > 0) {
            for (File file : externalFilesDirs) {
                //Check in where locations exist this internal folder of app, in only exist in one, means there are no
                //removable memory installed
                if (file != null && !file.equals(ContextCompat.getExternalFilesDirs(sContext,"temp_tagged_files") ) ) {
                    int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                    if(index >= 0){
                        //get absolute path of this folder, so later we can use
                        //to construct the URI for a DocumentFIle object.
                        String path = file.getAbsolutePath().substring(0, index);
                        try {
                            path = new File(path).getCanonicalPath();
                            Log.w(TAG, "getting canonical path: " + path);
                        } catch (IOException e) {
                            Crashlytics.logException(e);
                            // Keep non-canonical path.
                        }
                        paths.add(path);
                    }
                }
            }
        }

        return paths.toArray(new String[paths.size()]);
    }

    /**
     * Exposes single method to apply new cover
     * or remove existent one
     * @param cover new cover to apply, pass null
     *              to remove current cover.
     * @return true if successful, false otherwise
     */
    public ResultCorrection applyCover(@Nullable byte[] cover, String pathToFile) throws ReadOnlyFileException, IOException, TagException, InvalidAudioFrameException, CannotReadException {
        if(pathToFile == null)
            throw new NullPointerException("Path to file not set.");

        if(pathToFile.isEmpty())
            throw new IllegalArgumentException("Path must not be empty.");

        return applyCover(cover, new File(pathToFile));
    }

    /**
     * Exposes single method to apply new cover
     * or remove existent one
     * @param cover new cover to apply, pass null
     *              to remove current cover.
     * @return true if successful, false otherwise
     */
    public ResultCorrection applyCover(@Nullable byte[] cover, File file) throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException {
        return internalApplyCover(file,cover);
    }

    /**
     * Encapsulates the method to apply new cover,
     * taking care if file is store in SD or internal storage.
     * @param coverToApply new cover to apply, pass null
     *              to remove current cover.
     * @return true if successful, false otherwise
     */
    private ResultCorrection internalApplyCover(File file, byte[] coverToApply) throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {
        boolean isStoredInSd = sStorageHelper.isStoredInSD(file);
        ResultCorrection resultCorrection = new ResultCorrection();
        TrackDataLoader.TrackDataItem trackDataItem = readFile(file);


        if(isStoredInSd) {
            if(AndroidUtils.getUriSD(sContext) == null) {
                resultCorrection.code = COULD_NOT_GET_URI_SD_ROOT_TREE;
            }
            else{
                if(trackDataItem.cover != null && coverToApply != null && (coverToApply.length == trackDataItem.cover.length)){
                    resultCorrection.code = APPLIED_SAME_COVER;
                }
                else {
                    resultCorrection = applyCoverForDocumentFileObject(coverToApply, file);
                }
            }
        }
        else {
            if(trackDataItem.cover != null && coverToApply != null && (coverToApply.length == trackDataItem.cover.length)){
                resultCorrection.code = APPLIED_SAME_COVER;
            }
            else {
                resultCorrection = applyCoverForFileObject(coverToApply, file);
            }
        }

        return resultCorrection;
    }

    /**
     * Apply cover art for file stored in SD card
     * @param cover New cover as byte array, if null is passed, then
     *              delete current cover.
     * @return true if was succesful applied, false otherwise;
     */
    private ResultCorrection applyCoverForDocumentFileObject(byte[] cover, File file){
        //Tries to creates a temp file in non removable storage to work with it
        File tempFile = sStorageHelper.createTempFileFrom(file);
        ResultCorrection resultCorrection = new ResultCorrection();
        if(tempFile == null) {
            resultCorrection.code = COULD_NOT_CREATE_TEMP_FILE;
            return resultCorrection;
        }

        //Creates an audio file from temp file created in which
        //we can apply new data
        AudioFile audioFile = getAudioTaggerFile(tempFile);

        if(audioFile == null) {
            resultCorrection.code = COULD_NOT_CREATE_AUDIOFILE;
            return resultCorrection;
        }

        Tag tag = getTag(audioFile);

        if(tag == null){
            resultCorrection.code = COULD_NOT_READ_TAGS;
            return resultCorrection;
        }

        if(cover == null){
            try {
                //Delete current cover art
                tag.deleteArtworkField();
                audioFile.commit();
                resultCorrection.code = CURRENT_COVER_REMOVED;
            } catch (CannotWriteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                resultCorrection.code = COULD_NOT_APPLY_COVER;
                return resultCorrection;
            }
        }
        else {
            try {
                //Replace current cover art and apply the new one
                Artwork artwork = new AndroidArtwork();
                artwork.setBinaryData(cover);
                tag.deleteArtworkField();
                tag.setField(artwork);
                audioFile.commit();
                resultCorrection.code = NEW_COVER_APPLIED;
            } catch (FieldDataInvalidException  | CannotWriteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                resultCorrection.code = COULD_NOT_REMOVE_COVER;
                return resultCorrection;
            }
        }

        //Try to copy file to its original SD location
        int resultOfCopyBack = copyBack(tempFile, file);

        if(resultOfCopyBack != SUCCESS_COPY_BACK){
            resultCorrection.code = COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION;
            return resultCorrection;
        }

        deleteTempFile(tempFile);

        return resultCorrection;
    }

    /**
     * Apply coverart for file stored in non removable storage
     * (better known as internal storage).
     * @param cover New cover as byte array, if null is passed, then
     *              delete current cover.
     * @return true if was succesful applied, false otherwise;
     */
    private ResultCorrection applyCoverForFileObject(byte[] cover, File file){
        ResultCorrection resultCorrection = new ResultCorrection();
        AudioFile audioFile = getAudioTaggerFile(file);
        Tag currentTag = getTag(audioFile);

        if(cover == null){
            try {
                currentTag.deleteArtworkField();
                audioFile.commit();
                resultCorrection.code = CURRENT_COVER_REMOVED;
                return resultCorrection;
            } catch (CannotWriteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                resultCorrection.code = COULD_NOT_APPLY_COVER;
                return resultCorrection;
            }
        }
        else {
            Artwork artwork = new AndroidArtwork();
            artwork.setBinaryData(cover);
            try {
                currentTag.deleteArtworkField();
                currentTag.setField(artwork);
                audioFile.commit();
                resultCorrection.code = NEW_COVER_APPLIED;
                return resultCorrection;
            } catch (CannotWriteException | FieldDataInvalidException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                resultCorrection.code = COULD_NOT_REMOVE_COVER;
                return resultCorrection;
            }
        }
    }

    /**
     * Public common method to exposes rename file functionality both for files
     * stored in SD card or files stores in internal memory
     * @param currentFile The file toi rename
     * @param metadata The data to use for renaming
     * @return new absolute path to file;
     * @throws NullPointerException
     */
    @Nullable
    public String renameFile(File currentFile, String... metadata){
        boolean isStoredInSd = sStorageHelper.isStoredInSD(currentFile);

        if(isStoredInSd ){
            if (AndroidUtils.getUriSD(sContext) == null){
                return null;
            }
            return internalRenameDocumentFile(currentFile ,metadata);
        }
        else {
            return internalRenameFile(currentFile, metadata);
        }

    }

    /**
     * Rename file object using the data passed in the array
     * @param sourceFile FIle to rename
     * @param metadata Data to use as name, if is not empty
     * @return New complete file path as string
     */
    private String internalRenameDocumentFile(File sourceFile, String[] metadata) {
        if(metadata[0].isEmpty())
            return null;

        String title = metadata[0];
        String artistName = metadata[1];

        DocumentFile currentDocumentFile = getDocumentFile(sourceFile);
        if(currentDocumentFile == null){
            return null;
        }

        if(sameFilename(sourceFile, metadata)){
            return null;
        }

        if(!checkFileIntegrity(currentDocumentFile)){
            return null;
        }


        String currentParentPath = sourceFile.getParent();
        Log.d("parent path source file", currentParentPath);
        String newFilename = title + "." + getExtension(currentDocumentFile.getName());
        String newRelativeFilePath = currentParentPath + "/" + newFilename;

        String id = DocumentsContract.getDocumentId(getUriTree().getUri());

        //Check if new file document already exist
        id = id + getRelativePath(new File(newRelativeFilePath));
        Uri childUri = DocumentsContract.buildDocumentUriUsingTree(AndroidUtils.getUriSD(sContext), id);
        DocumentFile renamedDocument = DocumentFile.fromSingleUri(sContext, childUri);
        boolean wasRenamed = false;
        if(renamedDocument == null){
            return null;
        }

        //Another file with same name exists, then append artist to file name
        if(!renamedDocument.exists()){
            wasRenamed = currentDocumentFile.renameTo(newFilename);
        }
        else {
            //if artist tag was identificationFound
            if(!artistName.isEmpty()) {
                newFilename = title + " ( " + StringUtilities.sanitizeFilename(artistName) + " ) " + "." + getExtension(sourceFile);
            }
            else{
                newFilename = title +" ( "+ (int)Math.floor((Math.random()*100)+ 1) + " ) " + "." + getExtension(sourceFile);
            }

            wasRenamed = currentDocumentFile.renameTo(newFilename);
            newRelativeFilePath = currentParentPath + "/" + newFilename;

        }
        //Successful renamed then return new name including all file path.
        return wasRenamed ? newRelativeFilePath : null;
    }

    /**
     * Rename file object using the data passed in the array
     * @param sourceFile FIle to rename
     * @param metadata Data to use as name, if is not empty
     * @return New complete file path as string
     */
    private String internalRenameFile(File sourceFile, String[] metadata) {
        if(metadata[0].isEmpty())
            return null;

        String title = StringUtilities.sanitizeFilename(metadata[0]);
        String artistName = metadata[1];

        if(sameFilename(sourceFile, metadata)){
            return null;
        }

        if(!checkFileIntegrity(sourceFile)){
            return null;
        }

        boolean success =false;
        File renamedFile;
        String newParentPath = sourceFile.getParent();

        String newFilename = StringUtilities.sanitizeFilename(title) + "." + getExtension(sourceFile);
        String newAbsolutePath= newParentPath + "/" + newFilename;
        renamedFile = new File(newAbsolutePath);
        if(!renamedFile.exists()) {
            success = sourceFile.renameTo(renamedFile);
        }
        else {
            //if artist tag was identificationFound
            if(!artistName.isEmpty()) {
                newFilename = title + "(" + StringUtilities.sanitizeFilename(artistName) + ")" + "." + getExtension(sourceFile);
            }
            else{
                newFilename = title +"("+ (int)Math.floor((Math.random()*10)+ 1) +")"+ "." + getExtension(sourceFile);
            }
            newAbsolutePath = newParentPath + "/" + newFilename;
            renamedFile = new File(newAbsolutePath);
            success = sourceFile.renameTo(renamedFile);
        }

        //Successful renamed then return new name including all file path.
        return success ? newAbsolutePath : null;
    }

    /**
     * If filename is the same than title, then is not necessary rename it
     * @param sourceFile The source file to rename
     * @param metadata Title field tag to compare with file name
     * @return true if is necessary rename it, false otherwise
     */
    private static boolean sameFilename(File sourceFile, String... metadata){
        //Compare current file name against new title
        String currentFileName = sourceFile.getName();
        //Remove extension and get only name
        String currentFileNameWithoutExt = currentFileName.substring(0, currentFileName.length() - 4);
        return currentFileNameWithoutExt.equals(metadata[0]);
    }

    public static boolean checkFileIntegrity(DocumentFile file){
        return file.exists() && file.isFile() && file.canRead() && file.canWrite();
    }

    public static boolean checkFileIntegrity(File file) {
        return file.exists() && file.isFile() && file.canRead() && file.canWrite();
    }

    public static boolean checkFileIntegrity(String path) {
        return checkFileIntegrity(new File(path));
    }

        /**
         * Deletes file passed in paramete if exist.
         * @param file
         */
    private void deleteTempFile(File file){
        if(file != null && file.exists()){
            file.delete();
        }
    }

    public static class ResultCorrection{
        public String pathTofileUpdated = null;
        public int code = -1;
        public int allTagsApplied;
        public Track track;
        public HashMap<FieldKey, Object> tagsUpdated;
    }
}
