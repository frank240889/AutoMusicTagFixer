package mx.dev.franco.automusictagfixer.fixer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mx.dev.franco.automusictagfixer.BuildConfig;

/**
 * Helper class that wraps the functionality to
 * read and write metadata for audio files.
 */
public class AudioTagger {
    public static final int COULD_NOT_WRITE_TAGS = 200;
    private static final String TAG = AudioTagger.class.getName();
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
    public static final int SUCCESS = 23;

    //Constants to indicate that operation was successful
    //but previous and new tags were the same.
    public static final int APPLIED_SAME_TAGS = 24;
    public static final int APPLIED_SAME_COVER = 25;

    //No onIdentificationError is set
    public static final int NOT_SET = -1;
    private static final int SUCCESS_APPLY_COVER = 22;

    private Context mContext;
    private static final int BUFFER_SIZE = 131072;//->128Kb
    private static final float KILOBYTE = 1048576;


    //Used as a support for storage operations
    private StorageHelper mStorageHelper;

    /**
     * A single instance of this helper
     * @param context
     */
    public AudioTagger(Context context, StorageHelper storageHelper) {
        this(context);
        mStorageHelper = storageHelper;
    }
    /**
     * A single instance of this helper
     * @param context The context required to access system resources.
     */
    public AudioTagger(Context context) {
        mContext = context.getApplicationContext();
        TagOptionSingleton.getInstance().setAndroid(true);
        //Save genres as text, not as numeric codes
        TagOptionSingleton.getInstance().setWriteMp3GenresAsText(true);
        TagOptionSingleton.getInstance().setWriteMp4GenresAsText(true);
    }

    /**
     * @param pathToTargetFile The path of the file to apply new tags.
     * @param tags Tags to apply.
     * @param overWriteTags Option to indicate if current tags must overwrite
     *                      or only apply those missing.
     * @return resultCorrection object containing the state of operation.
     * @throws ReadOnlyFileException
     * @throws CannotReadException
     * @throws TagException
     * @throws InvalidAudioFrameException
     * @throws IOException
     */
    public ResultCorrection saveTags(String pathToTargetFile, Map<FieldKey, Object> tags, int overWriteTags)
            throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {

        if(pathToTargetFile == null || pathToTargetFile.isEmpty())
            throw new NullPointerException("Path to target file has not been set yet.");

        return saveTags(new File(pathToTargetFile), tags, overWriteTags);
    }

    /**
     * @param targetFile The file to apply new tags.
     * @param tags Tags to apply.
     * @param overWriteTags Option to indicate if current tags must overwrite
     *                      or only apply those missing.
     * @return resultCorrection object containing the state of operation.
     * @throws ReadOnlyFileException
     * @throws CannotReadException
     * @throws TagException
     * @throws InvalidAudioFrameException
     * @throws IOException
     */
    public ResultCorrection saveTags(File targetFile, Map<FieldKey, Object> tags, int overWriteTags)
            throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {

        if(targetFile == null)
            throw new NullPointerException("Target file has not been set yet.");

        return applyTags(targetFile, tags, overWriteTags);
    }

    /**
     * Creates the AudioFile object required by the library to read its tags.
     * @param file The file to read it tags.
     * @return trackDataItem containing the information of read tags.
     * @throws IOException
     * @throws TagException
     * @throws ReadOnlyFileException
     * @throws CannotReadException
     * @throws InvalidAudioFrameException
     */
    public AudioFields readFile(File file)
            throws IOException, TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException {
        if(file == null)
            throw new NullPointerException("Source file has not been set yet.");
        if(!file.exists())
            throw new FileNotFoundException("File does not exist");

        //AudioFile handled by jAudioTagger library
        AudioFile audioFile = AudioFileIO.read(file);
        if(audioFile == null)
            return null;

        return getData(audioFile);
    }

    /**
     * @param audioFile The object required by the library to read its tags.
     * @return trackDataItem object containing the info of track.
     * @throws ReadOnlyFileException
     * @throws CannotReadException
     * @throws TagException
     * @throws InvalidAudioFrameException
     * @throws IOException
     */
    private AudioFields getData(AudioFile audioFile){
        AudioFields audioFields = new AudioFields();

        File file = audioFile.getFile();
        String extension = getExtension(file);
        String mimeType = getMimeType(file);

        audioFields.extension = extension;
        audioFields.fileName = file.getName();
        audioFields.path = file.getParent();

        Tag tag = getTag(audioFile);
        AudioHeader audioHeader = audioFile.getAudioHeader();
        //Get header info and current tags
        audioFields.duration = getHumanReadableDuration(audioHeader.getTrackLength() + "");
        audioFields.bitrate = getBitrate(audioHeader.getBitRate());
        audioFields.frequency = getFrequency(audioHeader.getSampleRate());
        audioFields.resolution = getResolution(audioHeader.getBitsPerSample());
        audioFields.channels = audioHeader.getChannels();
        audioFields.fileType = audioHeader.getFormat();
        audioFields.fileSize = getFileSize(file.length());

        audioFields.title = tag.getFirst(FieldKey.TITLE);
        audioFields.artist = tag.getFirst(FieldKey.ARTIST);
        audioFields.album = tag.getFirst(FieldKey.ALBUM);
        audioFields.trackNumber = tag.getFirst(FieldKey.TRACK);
        audioFields.trackYear = tag.getFirst(FieldKey.YEAR);
        audioFields.genre = tag.getFirst(FieldKey.GENRE);

        audioFields.cover = getCover(audioFile);
        audioFields.imageSize = getStringImageSize(audioFields.cover);

        return audioFields;
    }

    /**
     * Calls {@link #readFile(File)}
     * @param path The path to the file to read it tags.
     * @return trackDataItem containing the information of read tags.
     * @throws IOException
     * @throws TagException
     * @throws ReadOnlyFileException
     * @throws CannotReadException
     * @throws InvalidAudioFrameException
     */
    public AudioFields readFile(String path)
            throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {
        if(path == null)
            throw new NullPointerException("Path to file has not been set yet.");
        return readFile(new File(path));
    }

    /**
     * Tries to apply new tags doing before validations to know
     * if file is stored in SD, or if current tags are same than news.
     * @param file The file to apply new tags.
     * @param tags New tags to apply.
     * @param overWriteTags Indicates if current tags must be overwritten or
     *                      only apply those missing.
     * @return
     * @throws ReadOnlyFileException
     * @throws IOException
     * @throws TagException
     * @throws InvalidAudioFrameException
     * @throws CannotReadException
     */
    private ResultCorrection applyTags(File file, Map<FieldKey, Object> tags, int overWriteTags)
            throws ReadOnlyFileException, IOException, TagException, InvalidAudioFrameException, CannotReadException {

        boolean isStoredInSd = mStorageHelper.isStoredInSD(file);

        ResultCorrection resultCorrection;

        if(isStoredInSd){
            if(getUriSD() == null) {
                resultCorrection = new ResultCorrection();
                resultCorrection.setCode(COULD_NOT_GET_URI_SD_ROOT_TREE);
            }
            else {
                //Hold which tags were applied to return in results.
                Map<FieldKey, Object> tagsToUpdate = isNeededUpdateTags(overWriteTags,file, tags);
                if(tagsToUpdate.isEmpty()){
                    resultCorrection = new ResultCorrection();
                    resultCorrection.setCode(APPLIED_SAME_TAGS);
                }
                else {
                    resultCorrection = applyTagsForDocumentFileObject(file, tagsToUpdate, overWriteTags);
                }
                resultCorrection.setTagsUpdated(tagsToUpdate);
            }
        }
        else {
            Map<FieldKey, Object> tagsToUpdate = isNeededUpdateTags(overWriteTags,file, tags);
            if(tagsToUpdate.isEmpty()){
                resultCorrection = new ResultCorrection();
                resultCorrection.setCode(APPLIED_SAME_TAGS);
            }
            else {
                resultCorrection = applyTagsForFileObject(file, tagsToUpdate, overWriteTags);
            }
            resultCorrection.setTagsUpdated(tagsToUpdate);
        }

        return resultCorrection;
    }

    /**
     * Check which tags needs to setChecked, comparing current
     * tags of file and new tags.
     * @param overrideAllTags Indicates the mode of comparision, if
     *                         mOverrideAllTags is true, will set all tags as needed to setChecked,
     *                         only if are not equal current than new; if mOverrideAllTags is false
     *                         will set only those missing in file.
     * @param file The file to apply tags.
     * @param newTags The new tags to apply.
     * @return A hashmap containing the tags that will be setChecked; it will be empty
     *          if all current tags and news are equals.
     * @throws TagException
     * @throws ReadOnlyFileException
     * @throws CannotReadException
     * @throws InvalidAudioFrameException
     * @throws IOException
     */
    private Map<FieldKey, Object> isNeededUpdateTags(int overrideAllTags, File file, Map<FieldKey, Object> newTags)
            throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException {
        AudioFile audioFile = AudioFileIO.read(file);
        Tag currentTags = getTag(audioFile);

        byte[] currentCover = (currentTags.getFirstArtwork() != null && currentTags.getFirstArtwork().getBinaryData() != null) ?
                currentTags.getFirstArtwork().getBinaryData() :
                null;

        Map<FieldKey, Object> tagsToUpdate = new ArrayMap<>();
        //Iterates over new values tag passed, to compare
        //against the values of current tag and setChecked only those
        //that are different than current
        for(Map.Entry entry : newTags.entrySet()){
            //For case of field cover, we need to compare the length of byte array
            if(entry.getKey() == FieldKey.COVER_ART){
                //Write missing tags only
                if(overrideAllTags == MODE_WRITE_ONLY_MISSING) {
                    if ((currentCover == null) || currentCover.length == 0) {
                        tagsToUpdate.put((FieldKey) entry.getKey(), entry.getValue());
                    }
                }
                //Overwrite tags, but last comparision is to check if new cover is same
                //than current, if is the same we don't setChecked the field
                else if ((currentCover == null) || currentCover.length == 0 || currentCover.length != ((byte[])entry.getValue()).length ) {
                    tagsToUpdate.put((FieldKey) entry.getKey(), entry.getValue());
                }
            }

            //Compare for other fields if current value tag exist, and if current value tags is different that new
            else {
                if(overrideAllTags == MODE_WRITE_ONLY_MISSING){
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

    /**
     * Gets the tag of audio file object, this tag contains the information
     * like title, artist, etc.
     * @param audioFile The file to read its tag.
     * @return The tag object of the file.
     */
    private Tag getTag(AudioFile audioFile){

        Tag tag = null;
        String mimeType = getMimeType(audioFile.getFile());
        String extension = getExtension(audioFile.getFile());

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
     * @return resultCorrection with the state of correction
     */
    private ResultCorrection applyTagsForDocumentFileObject(File file, Map<FieldKey,
            Object> tagsToApply, int overwriteTags) {
        ResultCorrection resultCorrection = new ResultCorrection();
        //Creates a temp file in non removable storage to work with it
        File tempFile = mStorageHelper.createTempFileFrom(file);

        if (tempFile == null) {
            resultCorrection.setCode(COULD_NOT_CREATE_TEMP_FILE);
        }
        else {
            //Try to create AudioFile
            AudioFile audioFile = getAudioTaggerFile(tempFile);

            if (audioFile == null) {
                resultCorrection.setCode(COULD_NOT_CREATE_AUDIOFILE);
            }
            else {
                //Set new tags and get ready to apply
                setNewTags(audioFile, tagsToApply, overwriteTags);

                //Apply tags to current temp file
                try {
                    audioFile.commit();

                    //Try to copy temp file with its news tags to its original location
                    int resultOfCopy = copyBack(tempFile, file);

                    if (resultOfCopy != SUCCESS_COPY_BACK) {
                        resultCorrection.setCode(resultOfCopy);
                    }
                    else {
                        resultCorrection.setCode(SUCCESS);
                    }
                } catch (CannotWriteException e) {
                    e.printStackTrace();
                    resultCorrection.setCode(COULD_NOT_APPLY_TAGS);
                }
                finally {
                    //Delete temp file from internal storage
                    deleteTempFile(tempFile);
                }
            }
        }
        return resultCorrection;
    }

    @Nullable
    public static byte[] getCover(@NonNull String pathToFile) {
        return getCover(new File(pathToFile));
    }

    @Nullable
    public static byte[] getCover(File file) {
        if(!file.exists())
            return null;

        try {
            AudioFile audioTaggerFile = AudioFileIO.read(file);
            return getCover(audioTaggerFile);
        }
        catch(IOException | CannotReadException | ReadOnlyFileException | InvalidAudioFrameException | TagException e){
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    private static byte[] getCover(AudioFile audioTaggerFile) {
        Tag tag = null;
        if (audioTaggerFile.getTag() == null)
            return null;

        tag = audioTaggerFile.getTag();

        if (tag.getFirstArtwork() == null)
            return null;

        if(tag.getFirstArtwork().getBinaryData() == null)
            return null;

        return tag.getFirstArtwork().getBinaryData();
    }

    /**
     * Apply tags for file stored in non removable external storage
     * (better known as internal storage).
     * @return true if successful, false otherwise
     */
    private ResultCorrection applyTagsForFileObject(File file, Map<FieldKey, Object> tagsToApply, int overwriteTags){
        ResultCorrection resultCorrection = new ResultCorrection();
        //Try to create AudioFile
        AudioFile audioFile = getAudioTaggerFile(file);

        if (audioFile == null) {
            resultCorrection.setCode(COULD_NOT_CREATE_AUDIOFILE);
        }
        else {
            //Put new values in its fields
            setNewTags(audioFile, tagsToApply, overwriteTags);

            try {
                audioFile.commit();
                resultCorrection.setCode(SUCCESS);
            }
            catch (CannotWriteException e) {
                e.printStackTrace();
                resultCorrection.setCode(COULD_NOT_APPLY_TAGS);
            }
        }

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
     * Sets new tag values into current tag of current audio file
     * @param audioFile The audio file to apply tags.
     * @param tagsToApply The tags to apply to he audio file object.
     * @param overwriteAllTags Indicates if all current tags ust be overwrote or write
     *                         only those missing.
     */
    private void setNewTags(AudioFile audioFile, Map<FieldKey, Object> tagsToApply, int overwriteAllTags){
        Tag currentTag = getTag(audioFile);
        String mimeType = getMimeType(audioFile.getFile());
        String extension = getExtension(audioFile.getFile());
        boolean isMp3 = ((mimeType.equals("audio/mpeg_mp3") || mimeType.equals("audio/mpeg") )
                && extension.toLowerCase().equals("mp3"));
        //remove old version of ID3 tags
        if (isMp3 && ((MP3File) audioFile).hasID3v1Tag()) {
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
                //in that case set the field with the value passed in the mNewTags Map
                else {
                    try {
                        if( currentTag.getFirst((FieldKey) entry.getKey()) == null ||
                                currentTag.getFirst((FieldKey) entry.getKey()).isEmpty()  ) {

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
     * Copy temp file that was created in internal storage
     * to its original location in SD card
     * @return The code corresponding to the state of copy the file to its original
     *                  location
     * @param correctedFile The file with new tags applied.
     * @param originalFile Original file that was corrected.
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
            DocumentFile newFile =  sourceDocumentFile.getParentFile().
                    createFile(getMimeType(correctedFile), correctedFile.getName() );
            //Destination data
            out = mContext.getContentResolver().openOutputStream(newFile.getUri());
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
     * @return sourceDocumentFile repressenting the original final in SD card.
     * @param sourceFile The file to trackSearch its name in SD card.
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
        //this file is not a file so we can not apply tags
        if(!sourceDocumentFile.isFile())
            return null;

        return sourceDocumentFile;
    }

    /**
     *  Gets the root document file object
     *  using the Uri of SD card.
     * @return The root Uri of SD card, if exist
     *         or null otherwise.
     */
    private DocumentFile getUriTree(){
        Uri uri = getUriSD();
        if(uri == null) {
            return null;
        }
        return DocumentFile.fromTreeUri(mContext, uri);
    }

    /**
     * Returns relative path of file
     * @param file The file to process
     * @return a string with the relative path
     *                  of this file
     */
    private String getRelativePath(File file){
        String baseFolder = getExtSdCardFolder(file);

        if (baseFolder == null) {
            return null;
        }

        String relativePath;
        try {
            String fullPath = file.getCanonicalPath();
            relativePath = fullPath.substring(baseFolder.length() + 1);
        } catch (IOException e) {
            Crashlytics.logException(e);
            return null;
        }

        return relativePath;
    }

    /**
     * Returns the relative path of
     * storage device in which is
     * located this file.
     * @param file The file to process
     * @return a string of the relative path of the
     *                  device in which is stored
     */
    private String getExtSdCardFolder(final File file) {
        String[] extSdPaths = getExtSdCardPaths();
        try {
            for (String extSdPath : extSdPaths) {
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
     * internal app folders, those that are stored id DATA directory,
     * from non removable storage and removable storage
     * @return An array of internal folders of app.
     */
    private String[] getExtSdCardPaths() {
        List<String> paths = new ArrayList<>();
        //Those folders are  created  if not exist
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(mContext,"temp_tagged_files");
        if (externalFilesDirs.length > 0) {
            for (File file : externalFilesDirs) {
                //Check in where locations exist this internal folder of app, in only exist in one, means there are no
                //removable memory installed
                if (file != null && !file.equals(ContextCompat.getExternalFilesDirs(mContext,"temp_tagged_files") ) ) {
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
     * @param cover new cover to apply, set null
     *              to remove current cover.
     * @return resultCorrection containing the result of operation.
     */
    public ResultCorrection applyCover(@Nullable byte[] cover, @NonNull String pathToTargetFile)
            throws ReadOnlyFileException, IOException, TagException, InvalidAudioFrameException, CannotReadException {
        if(pathToTargetFile == null)
            throw new NullPointerException("Path to target file not set.");

        if(pathToTargetFile.isEmpty())
            throw new IllegalArgumentException("Path must not be empty.");

        return applyCover(cover, new File(pathToTargetFile));
    }

    /**
     * Exposes single method to apply new cover
     * or remove existent one
     * @param cover new cover to apply, pass null
     *              to remove current cover.
     * @return resultCorrection containing the result of operation.
     */
    public ResultCorrection applyCover(@Nullable byte[] cover, File file)
            throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException {
        return internalApplyCover(file,cover);
    }

    /**
     * Applies new cover, taking care if file is store in SD or internal storage.
     * @param coverToApply new cover to apply, pass null
     *              to remove current cover.
     * @return resultCorrection containing the result of operation.
     */
    private ResultCorrection internalApplyCover(File file, byte[] coverToApply)
            throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {

        boolean isStoredInSd = mStorageHelper.isStoredInSD(file);

        ResultCorrection resultCorrection;
        AudioFields audioFields = readFile(file);


        if(isStoredInSd) {
            if(getUriSD() == null) {
                resultCorrection = new ResultCorrection();
                resultCorrection.setCode(COULD_NOT_GET_URI_SD_ROOT_TREE);
            }
            else{
                if(audioFields.cover != null && coverToApply != null &&
                        (coverToApply.length == audioFields.cover.length)){
                    resultCorrection = new ResultCorrection();
                    resultCorrection.setCode(SUCCESS);
                }
                else {
                    resultCorrection = applyCoverForDocumentFileObject(coverToApply, file);
                }
            }
        }
        else {
            if(audioFields.cover != null && coverToApply != null &&
                    (coverToApply.length == audioFields.cover.length)){
                resultCorrection = new ResultCorrection();
                resultCorrection.setCode(SUCCESS);
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
     *              it will be deleted current cover.
     * @return resultCorrection containing the result of operation.
     */
    private ResultCorrection applyCoverForDocumentFileObject(byte[] cover, File file){
        //Tries to creates a temp file in non removable storage to work with it
        File tempFile = mStorageHelper.createTempFileFrom(file);
        ResultCorrection resultCorrection = new ResultCorrection();
        if(tempFile == null) {
            resultCorrection.setCode(COULD_NOT_CREATE_TEMP_FILE);
        }
        else {

            //Creates an audio file from temp file created in which
            //we can apply new data
            AudioFile audioFile = getAudioTaggerFile(tempFile);

            if (audioFile == null) {
                resultCorrection.setCode(COULD_NOT_CREATE_AUDIOFILE);
            }
            else {
                Tag tag = getTag(audioFile);

                if (tag == null) {
                    resultCorrection.setCode(COULD_NOT_READ_TAGS);
                }
                else {
                    if (cover == null) {
                        try {
                            //Delete current cover art
                            tag.deleteArtworkField();
                            audioFile.commit();
                            //Try to copy file to its original SD location
                            int resultOfCopyBack = copyBack(tempFile, file);

                            if (resultOfCopyBack != SUCCESS_COPY_BACK) {
                                resultCorrection.setCode(COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION);
                            }

                            resultCorrection.setCode(SUCCESS);
                        } catch (CannotWriteException e) {
                            resultCorrection.setCode(COULD_NOT_APPLY_COVER);
                        }
                    } else {
                        try {
                            //Replace current cover art and apply the new one
                            Artwork artwork = new AndroidArtwork();
                            artwork.setBinaryData(cover);
                            tag.deleteArtworkField();
                            tag.setField(artwork);
                            audioFile.commit();

                            //Try to copy file to its original SD location
                            int resultOfCopyBack = copyBack(tempFile, file);

                            if (resultOfCopyBack != SUCCESS_COPY_BACK) {
                                resultCorrection.setCode(COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION);
                            }

                            resultCorrection.setCode(SUCCESS);
                        } catch (FieldDataInvalidException | CannotWriteException e) {
                            resultCorrection.setCode(COULD_NOT_REMOVE_COVER);
                        }
                    }
                }
            }
            deleteTempFile(tempFile);
        }

        return resultCorrection;
    }

    /**
     * Apply coverart for file stored in non removable storage
     * (better known as internal storage).
     * @param cover New cover as byte array, if null is passed, then
     *              delete current cover.
     * @return resultCorrection containing the result of operation.
     */
    private ResultCorrection applyCoverForFileObject(@Nullable byte[] cover, File file){
        ResultCorrection resultCorrection = new ResultCorrection();
        AudioFile audioFile = getAudioTaggerFile(file);
        Tag currentTag = getTag(audioFile);

        //When cover is null, then the current cover will be removed.
        if(cover == null){
            try {
                currentTag.deleteArtworkField();
                audioFile.commit();
                resultCorrection.setCode(SUCCESS);
            } catch (CannotWriteException e) {
                resultCorrection.setCode(COULD_NOT_APPLY_COVER);
            }
        }
        else {
            Artwork artwork = new AndroidArtwork();
            artwork.setBinaryData(cover);
            try {
                currentTag.deleteArtworkField();
                currentTag.setField(artwork);
                audioFile.commit();
                resultCorrection.setCode(SUCCESS);
            } catch (CannotWriteException | FieldDataInvalidException e) {
                resultCorrection.setCode(COULD_NOT_REMOVE_COVER);
            }
        }
        return resultCorrection;
    }

    /**
     * Public common method to exposes rename file functionality both for files
     * stored in SD card or files stores in internal memory
     * @param currentFile The file to rename
     * @param newName The new name for the file, without extension.
     * @return string of new absolute path to file or null if could not be renamed;
     */
    public String renameFile(File currentFile, String newName){
        boolean isStoredInSd = mStorageHelper.isStoredInSD(currentFile);

        String newFileName;

        if(isStoredInSd && getUriSD() != null){
            newFileName = internalRenameDocumentFile(currentFile ,newName);
        }
        else {
            newFileName =  internalRenameFile(currentFile, newName);
        }

        return newFileName;
    }

    /**
     * Renames document file object.
     * @param sourceFile File to rename
     * @param newName The new name for the file, without extension.
     * @return string of new absolute path to file or null if could not be renamed;
     */
    private String internalRenameDocumentFile(File sourceFile, String newName) {
        if(newName.isEmpty())
            throw new IllegalArgumentException("Cannot rename a file with an empty string.");

        if(sameFilename(sourceFile, newName)){
            return sourceFile.getAbsolutePath();
        }

        if(!checkFileIntegrity(sourceFile)){
            return null;
        }

        DocumentFile currentDocumentFile = getDocumentFile(sourceFile);

        if(currentDocumentFile == null)
            return null;

        String currentParentPath = sourceFile.getParent();
        String newFilename = newName + "." + getExtension(sourceFile.getName());
        String newRelativeFilePath = currentParentPath + "/" + newFilename;

        Uri uriTree = getUriTree().getUri();

        if(uriTree == null)
            return null;

        String id = DocumentsContract.getDocumentId(uriTree);

        //Check if new file document already exist
        id = id + getRelativePath(new File(newRelativeFilePath));
        Uri childUri = DocumentsContract.buildDocumentUriUsingTree(getUriSD(), id);
        DocumentFile renamedDocument = DocumentFile.fromSingleUri(mContext, childUri);
        boolean wasRenamed;

        if(renamedDocument == null){
            return null;
        }

        //Another file with same name exists, then append artist to file name
        if(!renamedDocument.exists()){
            wasRenamed = currentDocumentFile.renameTo(newFilename);
        }
        else {
            Date date = new Date();
            DateFormat now = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            newFilename = newName +"("+ now.format(date) +")"+ "." + getExtension(sourceFile);
            wasRenamed = currentDocumentFile.renameTo(newFilename);
            newRelativeFilePath = currentParentPath + "/" + newFilename;

        }
        //Successful renamed then return new name including all file path.
        return wasRenamed ? newRelativeFilePath : null;
    }

    /**
     * Renames file object.
     * @param sourceFile The file to rename
     * @param newName The new name for the file, without extension.
     * @return string of new absolute path to file or null if could not be renamed;
     */
    private String internalRenameFile(File sourceFile, String newName) {
        if(newName.isEmpty())
            throw new IllegalArgumentException("Cannot rename a file with an empty string.");

        if(sameFilename(sourceFile, newName)){
            return sourceFile.getAbsolutePath();
        }

        if(!checkFileIntegrity(sourceFile)){
            return null;
        }

        boolean success;
        File renamedFile;
        String newParentPath = sourceFile.getParent();

        String newFilename = StringUtilities.sanitizeFilename(newName) + "." + getExtension(sourceFile);
        String newAbsolutePath = newParentPath + "/" + newFilename;
        renamedFile = new File(newAbsolutePath);
        if(!renamedFile.exists()) {
            success = sourceFile.renameTo(renamedFile);
        }
        else {
            //Get and format date
            Date date = new Date();
            DateFormat now = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            newFilename = newName +"("+ now.format(date) +")"+ "." + getExtension(sourceFile);
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
     * @param newName The new name to compare with the current name of sourceFile
     * @return true if is necessary rename it, false otherwise
     */
    private static boolean sameFilename(File sourceFile, String newName){
        //Compare current file name against new title
        String currentFileName = sourceFile.getName();
        //Remove extension and get only name
        String currentFileNameWithoutExt = currentFileName.substring(0, currentFileName.length() - 4);
        return currentFileNameWithoutExt.equals(newName);
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

    public static String getExtension(String absolutePath){
        if(absolutePath == null || absolutePath.isEmpty())
            return null;

        return getExtension(new File(absolutePath));
    }

    public static String getExtension(File file){
        if(file == null)
            return null;

        int lastIndexOfDot = file.getName().lastIndexOf(".") + 1;
        String ext = file.getName().substring(lastIndexOfDot);
        //get file extension, extension must be in lowercase
        return ext.toLowerCase();
    }

    public static String getMimeType(String absolutePathSourceFile){
        String ext = getExtension(absolutePathSourceFile);
        if(ext == null)
            return null;
        //get type depending on extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    public static String getMimeType(File file){
        String ext = getExtension(file);
        if(ext == null)
            return null;
        //get type depending on extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    public static String getFrequency(String freq){
        if(freq == null || freq.isEmpty())
            return "0 Khz";

        float f = Float.parseFloat(freq);
        float f2 = f / 1000f;
        return f2+ " Khz";
    }

    public static String getResolution(int res){
        return res + " bits";
    }

    public static String getBitrate(String bitrate) {

        if (bitrate != null && !bitrate.equals("") && !bitrate.equals("0") ){
            // int bitrateInt = Integer.parseInt(bitrate);
            return bitrate + " Kbps";
        }
        return "0 Kbps";
    }

    /**
     * Gets file size in megabytes
     * @param size File size
     * @return formatted file size string in kilobytes.
     */
    @Nullable
    public static String getFileSize(long size){
        if(size <= 0)
            return null;

        float s = size ;/// KILOBYTE;
        String str = String.valueOf(s);
        //int l = str.length();
        String readableSize = "";
        /*if(l > 4)
            readableSize = str.substring(0,4);
        else
            readableSize =str.substring(0,3);*/
        readableSize += " kb";

        return readableSize;
    }

    public static String getFileSize(String path){
        File file = new File(path);
        if(!checkFileIntegrity(file))
            return "0";

        return getFileSize(file.length());
    }

    /**
     * Gets image dimensions.
     * @param cover Cover as byte array.
     * @return Size of image as string or null if could not be read.
     */
    @Nullable
    public static String getStringImageSize(byte[] cover){
        String size = null;
        if(cover != null && cover.length > 0) {
            try {
                Bitmap bitmapDrawable = BitmapFactory.decodeByteArray(cover, 0, cover.length);
                size = bitmapDrawable.getHeight() + " * " + bitmapDrawable.getWidth();
            }
            catch (Exception ignored){}
        }
        return size;
    }

    /**
     * Formats duration to human readable string.
     * @param duration duration in seconds
     * @return formatted string duration or null if
     * duration could not be parsed.
     */
    @Nullable
    public static String getHumanReadableDuration(String duration){
        String readableDuration = null;
        if(duration == null || duration.isEmpty())
            return null;

        try {
            int d = Integer.parseInt(duration);
            int minutes;
            int seconds;
            minutes = (int) Math.floor(d / 60f);
            seconds = d % 60;
            readableDuration = minutes + "\'" + (seconds < 10 ? ("0" + seconds) : seconds) + "\"";
        }
        catch (NumberFormatException ignored) { }

        return readableDuration;
    }

    /**
     * Get the uri tree from persistence storage.
     * @return An URI object.
     */
    private Uri getUriSD(){
        String uriString = mContext.
                getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).
                getString("uri_tree", null);

        if(uriString == null)
            return null;
        return Uri.parse(uriString);
    }

    /**
     * Deletes file passed as parameter.
     * @param file The file to delete.
     */
    private void deleteTempFile(File file){
        if(file != null && file.exists()){
            file.delete();
        }
    }

    /**
     * Generic class for operations result of {@link AudioTagger}
     */
    public static abstract class AudioTaggerResult {
        private int code;

        AudioTaggerResult(){}

        public AudioTaggerResult(int code) {
            this();
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }

    /**
     * Container class that holds
     * the result of correction
     */
    public static class ResultCorrection extends AudioTaggerResult {
        public Map<FieldKey, Object> tagsUpdated;

        public ResultCorrection(){}

        public ResultCorrection(int code, Map<FieldKey, Object> tagsUpdated) {
            super(code);
            this.tagsUpdated = tagsUpdated;
        }

        public Map<FieldKey, Object> getTagsUpdated() {
            return tagsUpdated;
        }

        public void setTagsUpdated(Map<FieldKey, Object> tagsUpdated) {
            this.tagsUpdated = tagsUpdated;
        }
    }

    public static class AudioFields extends AudioTaggerResult {
        public String title = "";
        public String artist = "";
        public String album = "";
        public String trackNumber = "";
        public String trackYear = "";
        public String genre = "";
        public byte[] cover = null;

        public String fileName = "";
        public String path = "";

        public String duration = "";
        public String bitrate = "";
        public String frequency = "";
        public String resolution = "";
        public String channels = "";
        public String fileType = "";
        public String extension = "";
        public String mimeType = "";
        public String imageSize = "Sin carátula.";
        public String fileSize = "";

        public AudioFields(){
            super(SUCCESS);
        }
        public AudioFields(int code){
            super(code);
        }

        public AudioFields(String title,
                           String artist,
                           String album,
                           String trackNumber,
                           String trackYear,
                           String genre,
                           byte[] cover,
                           String fileName,
                           String path,
                           String duration,
                           String bitrate,
                           String frequency,
                           String resolution,
                           String channels,
                           String fileType,
                           String extension,
                           String mimeType,
                           String imageSize,
                           String fileSize) {
            this();
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.trackNumber = trackNumber;
            this.trackYear = trackYear;
            this.genre = genre;
            this.cover = cover;
            this.fileName = fileName;
            this.path = path;
            this.duration = duration;
            this.bitrate = bitrate;
            this.frequency = frequency;
            this.resolution = resolution;
            this.channels = channels;
            this.fileType = fileType;
            this.extension = extension;
            this.mimeType = mimeType;
            this.imageSize = imageSize;
            this.fileSize = fileSize;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public String getTrackNumber() {
            return trackNumber;
        }

        public void setTrackNumber(String trackNumber) {
            this.trackNumber = trackNumber;
        }

        public String getTrackYear() {
            return trackYear;
        }

        public void setTrackYear(String trackYear) {
            this.trackYear = trackYear;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public byte[] getCover() {
            return cover;
        }

        public void setCover(byte[] cover) {
            this.cover = cover;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getBitrate() {
            return bitrate;
        }

        public void setBitrate(String bitrate) {
            this.bitrate = bitrate;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public String getChannels() {
            return channels;
        }

        public void setChannels(String channels) {
            this.channels = channels;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getImageSize() {
            return imageSize;
        }

        public void setImageSize(String imageSize) {
            this.imageSize = imageSize;
        }

        public String getFileSize() {
            return fileSize;
        }

        public void setFileSize(String fileSize) {
            this.fileSize = fileSize;
        }
    }

    public static class ResultFileRename extends AudioTaggerResult{
        private String filename;
        private String path;
        private String fullpath;

        public ResultFileRename(int code, String filename, String path, String fullpath) {
            super(code);
            this.filename = filename;
            this.path = path;
            this.fullpath = fullpath;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFullpath() {
            return fullpath;
        }

        public void setFullpath(String fullpath) {
            this.fullpath = fullpath;
        }
    }

    /**
     * Created by franco on 12/01/18.
     */

    public static class StorageHelper {
        private Context mContext;
        private static StorageHelper sStorage;
        private SparseArray<String> mBasePaths = new SparseArray<>();
        private static final String PRIVATE_TEMP_FOLDER = "temp_tagged_files";
        private StorageHelper(Context context){
            mContext = context.getApplicationContext();
        }

        public static synchronized StorageHelper getInstance(Context context){
            if(sStorage == null) {
                sStorage = new StorageHelper(context);
            }

            return sStorage;
        }

        public int getNumberAvailableMediaStorage (){
            return ContextCompat.getExternalFilesDirs(mContext, null).length;
        }

        public boolean isPresentRemovableStorage(){
            return (mBasePaths.size() > 1);
        }

        /**
         * Detect number of storage available.
         */
        public StorageHelper detectStorage(){
            File[] storage = ContextCompat.getExternalFilesDirs(mContext, PRIVATE_TEMP_FOLDER);

            int numberMountedStorage = 0;

            for(File s : storage){
                //When SD card is removed sometimes storage hold a reference to this
                //folder, so if the reference is null, means the storage has unmounted or removed
                //and is not available anymore
                if(s != null) {
                    int i = s.getPath().lastIndexOf("/Android/data");
                    String basePath = s.getPath().substring(0,i);
                    mBasePaths.put(numberMountedStorage,basePath);
                    numberMountedStorage++;
                    Log.d("storage", basePath);
                }
            }
            return this;
        }

        /**
         * Returns the base path of available storage.
         * @return An {@link SparseArray} containing the base paths.
         */
        public SparseArray<String> getBasePaths(){
            return mBasePaths;
        }

        /**
         * Gets current available size
         * @return available size of current storage
         */
        private static long getInternalAvailableSize(){
            return Environment.getExternalStorageDirectory().getTotalSpace();
        }

        /**
         * Creates a temp file in external non-removable storage,
         * more known as shared Storage or internal storage
         * @param sourceFile The source file to copy.
         * @return The copy of file or null if could not be created.
         */
        public File createTempFileFrom(File sourceFile) {

            //Before create temp file, check if exist enough space,
            //to ensure we can perform correctly the operations, lets take the triple size of source file
            //because operations of AudioTagger library.
            long availableSize = getInternalAvailableSize();
            long fileSize = sourceFile.getTotalSpace();
            if(availableSize < (fileSize * 3) ) {

                return null;
            }


            // Create a path where we will place our private file on non removable external
            // storage.
            File externalNonRemovableDevicePath = ContextCompat.
                    getExternalFilesDirs(mContext, PRIVATE_TEMP_FOLDER)[0];

            File fileDest = new File(externalNonRemovableDevicePath, sourceFile.getName());

            FileChannel inChannel = null;
            FileChannel outChannel = null;

            try {
                inChannel = new FileInputStream(sourceFile).getChannel();
                outChannel = new FileOutputStream(fileDest).getChannel();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                return null;
            }

            //Copy data
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (inChannel != null)
                    try {
                        inChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                if (outChannel != null)
                    try {
                        outChannel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
            }
            return fileDest;
        }

        public boolean isStoredInSD(File file){
            return internalIsStoredInSD(file);
        }

        /**
         * Check if file is stored on SD card or Non removable storage.
         * @return True if file is stored in SD, false otherwise.
         */
        private boolean internalIsStoredInSD(File file){
            SparseArray<String> basePaths =  sStorage.getBasePaths();
            int availableStorage = basePaths.size();
            //If there are only one storage, no need to check
            // where is stored file.
            if(availableStorage < 2)
                return false;

            //The position 0 belongs to non removable external storage.
            for(int d = 0  ; d < availableStorage ; d++){
                if(d == 0 && file.getParent().contains(basePaths.get(d)) ){
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Created by franco on 21/06/17.
     * Helper class containing some useful
     * static methods for validating strings
     */

    public static final class StringUtilities {
        /**
         *
         * @param str is the input entered by user
         * @return true if string is empty, false otherwise
         */
        public static boolean isFieldEmpty(String str) {
            return str.isEmpty();
        }

        /**
         *
         * @param dirtyString
         * We replace all invalid characters because
         * compatibility problems when showing the information
         * about song
         * @return sanitized string
         */
        public static String sanitizeString(String dirtyString) {
            return dirtyString.replaceAll("[^\\w\\s()&_\\-\\]\\[\'#.:$]", "");
        }

        /**
         *
         * @param str is the input entered by user
         * @return true string contains another chararacter, false otherwise
         */
        public static boolean hasNotAllowedCharacters(String str){
            Pattern pattern = Pattern.compile("[^\\w\\s()&_\\-\\]\\[\'#.:$]");
            Matcher matcher = pattern.matcher(str);
            return matcher.find();
        }

        public static String sanitizeFilename(String dirtyFileName){
            if(dirtyFileName != null && !dirtyFileName.equals(""))
                return dirtyFileName.replaceAll("[^\\w\\s()&_\\-\\]\\[\'#.:$/]", "");

            return "";
        }

        /**
         *
         * @param str is the input entered by user
         * @return trimmed string
         */
        public static String trimString(String str){
            return str.trim();
        }
    }
}
