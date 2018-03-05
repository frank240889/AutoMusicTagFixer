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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.list.AudioItem;

import static mx.dev.franco.automusictagfixer.list.AudioItem.getExtension;

/**
 * Created by franco on 16/01/18.
 * This class was created to delegate
 * the responsibility of update tags for audiofiles,
 * because implies additional functionality
 * when files are stored on sd card.
 *
 * Is implemented by using part of Storage Access Framework
 * because is the only way to modify data when is stored
 * in SD.
 */

public final class TaggerHelper {
    private static final String TAG = TaggerHelper.class.getName();

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

    //Constants to indicate a successful operation
    public static final int CURRENT_COVER_REMOVED = 20;
    public static final int NEW_COVER_APPLIED = 21;
    public static final int APPLIED_ONLY_MISSING_TAGS = 22;
    public static final int APPLIED_ALL_TAGS = 23;

    //Constants to indicate that operation was successful
    //but previous and new tags were the same.
    public static final int APPLIED_SAME_TAGS = 24;
    public static final int APPLIED_SAME_COVER = 25;

    //No error is set
    public static final int NOT_SET = -1;


    private static TaggerHelper sTaggerHelper;
    private static Context sContext;
    private static final int BUFFER_SIZE = 1024;

    //Source file to work with
    private File mSourceFile;
    //Flag to hold state of current operation
    private int mError = NOT_SET;
    //Audio file handled by JAudioTagger library
    private AudioFile mAudioTaggerFile;
    //Headers of current audio file
    private AudioHeader mAudioHeader;
    //Flag to indicate if current file is mp3 or not
    private boolean mIsMp3 = false;
    //Current tags read when initSourceFile() is called
    private Tag mCurrentTag = null;
    private byte[] mCurrentArtwork = null;
    //HashMap that stores new tags to apply
    private HashMap<FieldKey,Object> mNewTags = null;
    //HashMap to store temporally only those tags to apply.
    private HashMap<FieldKey, Object> mFieldsToUpdate = new HashMap<>();

    private int mOverrideAllTags = MODE_WRITE_ONLY_MISSING;


    /**
     * A single instance of this helper
     * @param context
     */
    private TaggerHelper(Context context) {
        sContext = context.getApplicationContext();
        TagOptionSingleton.getInstance().setAndroid(true);
        //Save genres as text, not as numeric codes
        TagOptionSingleton.getInstance().setWriteMp3GenresAsText(true);
        TagOptionSingleton.getInstance().setWriteMp4GenresAsText(true);
    }

    /**
     * Creates a singleton of this class
     * @param context The app context needed to create this singleton
     * @return TaggerHelper singleton
     */
    public static TaggerHelper getInstance(Context context) {
        if (sTaggerHelper == null)
            sTaggerHelper = new TaggerHelper(context);

        return sTaggerHelper;
    }

    /**
     * Sets the file to work with.
     * @param file The input file to work with.
     * @return TaggerHelper object to chain calls to subsequent methods
     */
    public TaggerHelper setSourceFile(File file){
        mSourceFile = file;
        return sTaggerHelper;
    }

    /**
     * Exposes internal method to initialize audio file
     * @return TaggerHelper singleton
     */
    public TaggerHelper initSourceFile() throws ReadOnlyFileException,
            IOException,
            TagException,
            InvalidAudioFrameException,
            CannotReadException {
        internalInitSourceFile();
        return this;
    }

    /**
     * Retrieves data from source file
     * and initializes tag, header,
     * file type and audio file
     */
    private void internalInitSourceFile() throws TagException,
            ReadOnlyFileException,
            CannotReadException,
            InvalidAudioFrameException,
            IOException {
        if(mSourceFile == null)
            throw new NullPointerException("Source file has not been set yet.");

        //Get extension and type of current song
        String extension = AudioItem.getExtension(mSourceFile);
        String mimeType = AudioItem.getMimeType(mSourceFile);
        mIsMp3 = (mimeType.equals("audio/mpeg_mp3") || mimeType.equals("audio/mpeg") ) && extension.toLowerCase().equals("mp3");

        //AudioFile handled by jAudioTagger library
        mAudioTaggerFile = AudioFileIO.read(mSourceFile);

        //Headers for mp3 files differs than other formats.
        mAudioHeader = mIsMp3 ? ((MP3File) mAudioTaggerFile).getMP3AudioHeader() : mAudioTaggerFile.getAudioHeader() ;
        //Get current tag
        mCurrentTag = internalGetTag(mAudioTaggerFile);

        //Get current artwork, if has.
        mCurrentArtwork = (mCurrentTag.getFirstArtwork() != null && mCurrentTag.getFirstArtwork().getBinaryData() != null) ?
                mCurrentTag.getFirstArtwork().getBinaryData() :
                null;

    }

    /**
     * Return current read tag
     * @return Read tag
     */
    public Tag getCurrentTag(){
        return mCurrentTag;
    }

    /**
     * Return audio header for this audio file
     * @return
     */
    public AudioHeader getAudioHeader(){
        return mAudioHeader;
    }

    /**
     * Return current read Audio File object
     * @return
     */
    public AudioFile getAudioTaggerFile(){
        return mAudioTaggerFile;
    }

    /**
     * Gets current artwork
     * @return current artwork or null if does not exist
     */
    public byte[] getCurrentArtwork(){
        return mCurrentArtwork;
    }

    /**
     *  Gets if current audio file is mp3 or not
     * @return true if f this file is mp3 or not
     */
    public boolean isMp3(){
        return mIsMp3;
    }

    /**
     * Sets new tags to apply to current audio file
     * @param newTags The HashMap that contains new tags
     * @return TaggerHelper object to chain calls to subsequent methods
     */
    public TaggerHelper setTags(HashMap newTags) {
        mNewTags = newTags;
        return sTaggerHelper;
    }

    /**
     * Common method to apply tags both for files stored
     * in SD or in internal storage, with extra parameter
     * @param overWriteAllTags Indicates overwrite
     *                         all tags or not
     * @return true if successful, false otherwise
     */

    public boolean applyTags(int overWriteAllTags){
        mOverrideAllTags = overWriteAllTags;
        return applyTags();
    }

    /**
     * Common method to apply tags both for files stored
     * in SD or in internal storage
     * @return true if operation was successful
     */
    public boolean applyTags() {
        boolean isStoredInSd = StorageHelper.getInstance(sContext).isStoredInSD(mSourceFile);

        if(isStoredInSd && Constants.URI_SD_CARD == null) {
            setMessageCode(COULD_NOT_GET_URI_SD_ROOT_TREE);
            return false;
        }

        boolean isNeedToApplyTags = isNeedUpdateTags();

        if(!isNeedToApplyTags){
            setMessageCode(APPLIED_SAME_TAGS);
            return true;
        }

        if(isStoredInSd){
            return applyTagsForDocumentFileObject();
        }
        else {
            return applyTagsForFileObject();
        }
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
    private boolean applyTagsForDocumentFileObject() {
        boolean success = false;

        //Creates a temp file in non removable storage to work with it
        File tempFile = StorageHelper.getInstance(sContext).createTempFileFrom(mSourceFile);

        if (tempFile == null) {
            setMessageCode(COULD_NOT_CREATE_TEMP_FILE);
            return success;
        }

        //Try to create AudioFile
        AudioFile audioFile = getAudioTaggerFile(tempFile);

        if (audioFile == null) {
            setMessageCode(COULD_NOT_CREATE_AUDIOFILE);
            return success;
        }

        //remove old version of ID3 tags
        if (mIsMp3 && ((MP3File) audioFile).hasID3v1Tag()) {
            //Log.d("removed ID3v1","remove ID3v1");
            try {
                ((MP3File) audioFile).delete(((MP3File) audioFile).getID3v1Tag());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Set new tags and get ready to apply
        setTagFields(audioFile);

        //Apply tags to current temp file
        try {
            audioFile.commit();
        } catch (CannotWriteException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
            setMessageCode(COULD_NOT_APPLY_TAGS);
            mFieldsToUpdate.clear();
            return false;
        }

        //Try to copy temp file with its news tags to its original location
        success = copyBack(tempFile);

        if (!success) {
            setMessageCode(COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION);
            mFieldsToUpdate.clear();
        }
        else {
            if(mOverrideAllTags == MODE_OVERWRITE_ALL_TAGS)
                setMessageCode(APPLIED_ALL_TAGS);
            else
                setMessageCode(APPLIED_ONLY_MISSING_TAGS);

            //Init source file again to get updated data, and can use getter methods
            //for getTag, getHeader and getArtwork.
            try {
                internalInitSourceFile();
            } catch (TagException | ReadOnlyFileException | CannotReadException | InvalidAudioFrameException | IOException e) {
                e.printStackTrace();
            }
        }

        //Delete temp file from internal storage
        deleteTempFile(tempFile);

        return success;
    }

    /**
     * Apply tags for file stored in non removable external storage
     * (better known as internal storage).
     * @return true if successful, false otherwise
     */
    private boolean applyTagsForFileObject(){

        //Put new values in its fields
        setTagFields(null);
        //remove old version of ID3 tags
        if (mIsMp3 && ((MP3File) mAudioTaggerFile).hasID3v1Tag()) {
            try {
                ((MP3File) mAudioTaggerFile).delete(((MP3File) mAudioTaggerFile).getID3v1Tag());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            mAudioTaggerFile.commit();
            if(mOverrideAllTags == MODE_OVERWRITE_ALL_TAGS)
                setMessageCode(APPLIED_ALL_TAGS);
            else
                setMessageCode(APPLIED_ONLY_MISSING_TAGS);

            internalInitSourceFile();

            return true;
        }
        catch (CannotWriteException | IOException | CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
            e.printStackTrace();
            setMessageCode(COULD_NOT_APPLY_TAGS);
            mFieldsToUpdate.clear();
            return false;
        }
    }

    /**
     * Put new tag values into current tag of current audio file
     * @param audioFile
     */
    private void setTagFields(AudioFile audioFile){
        Tag currentTag = audioFile == null ? mCurrentTag :internalGetTag(audioFile);
        //WRITE ONLY MISSING TAGS
        if(mOverrideAllTags == MODE_WRITE_ONLY_MISSING){
            for(Map.Entry entry : mFieldsToUpdate.entrySet()){
                //check the case for cover art
                if(entry.getKey() == FieldKey.COVER_ART) {

                    if (currentTag.getFirstArtwork() == null ||
                            currentTag.getFirstArtwork().getBinaryData() == null ||
                            currentTag.getFirstArtwork().getBinaryData().length == 0) {

                        try {
                            currentTag.deleteArtworkField();
                            Artwork artwork = new AndroidArtwork();
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
                    }
                }

            }
        }

        //OVERWRITE ALL TAGS
        else {
            for(Map.Entry entry : mFieldsToUpdate.entrySet()){
                if(entry.getKey() == FieldKey.COVER_ART){
                    //check the case for cover art
                    Artwork artwork = new AndroidArtwork();
                    artwork.setBinaryData((byte[])entry.getValue());
                    try {
                        currentTag.deleteArtworkField();
                        currentTag.setField(artwork);
                    } catch (FieldDataInvalidException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        currentTag.setField((FieldKey) entry.getKey(), (String) entry.getValue());

                    } catch (FieldDataInvalidException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

    }

    public HashMap<FieldKey, Object> getUpdatedFields(){
        return mFieldsToUpdate;
    }

    /**
     * Public common method to exposes rename file functionality both for files
     * stored in SD card or files stores in internal memory
     * @param currentFile
     * @param context
     * @param metadata
     * @return
     * @throws NullPointerException
     */
    @Nullable
    public static String renameFile(File currentFile, Context context, String... metadata){
        boolean isStoredInSd = StorageHelper.getInstance(context.getApplicationContext()).isStoredInSD(currentFile);

        if(isStoredInSd ){
              if (Constants.URI_SD_CARD == null){
                  return null;
              }
            return internalRenameDocumentFile(currentFile, context.getApplicationContext() ,metadata);
        }
        else {
            return internalRenameFile(currentFile, metadata);
        }

    }

    /**
     * Rename file object using the data passed in the array
     * @param sourceFile FIle to rename
     * @param metadata Data to use as name, if is not empty
     * @param context Context used by SAF
     * @return New complete file path as string
     */
    private static String internalRenameDocumentFile(File sourceFile, Context context, String[] metadata) {
        if(metadata[0].isEmpty())
            return null;

        String title = metadata[0];
        String artistName = metadata[1];

        DocumentFile currentDocumentFile = TaggerHelper.getInstance(context).internalGetDocumentFile(sourceFile);
        if(currentDocumentFile == null){
            return null;
        }

        if(!needsToBeRenamed(sourceFile, metadata)){
            return null;
        }

        if(!checkFileIntegrity(currentDocumentFile)){
            return null;
        }


        String currentParentPath = sourceFile.getParent();
        Log.d("parent path source file", currentParentPath);
        String newFilename = title + "." + getExtension(currentDocumentFile.getName());
        String newRelativeFilePath = currentParentPath + "/" + newFilename;

        String id = DocumentsContract.getDocumentId(TaggerHelper.getInstance(context).getUriTree().getUri());

        //Check if new file document already exist
        id = id + TaggerHelper.getInstance(context).getRelativePath(new File(newRelativeFilePath));
        Uri childUri = DocumentsContract.buildDocumentUriUsingTree(Constants.URI_SD_CARD, id);
        DocumentFile renamedDocument = DocumentFile.fromSingleUri(context, childUri);
        boolean wasRenamed = false;
        if(renamedDocument == null){
            return null;
        }

        //Another file with same name exists, then append artist to file name
        if(!renamedDocument.exists()){
            wasRenamed = currentDocumentFile.renameTo(newFilename);
        }
        else {
            //if artist tag was found
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
    private static String internalRenameFile(File sourceFile, String[] metadata) {
        if(metadata[0].isEmpty())
            return null;

        String title = StringUtilities.sanitizeFilename(metadata[0]);
        String artistName = metadata[1];

        if(!needsToBeRenamed(sourceFile, metadata)){
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
            //if artist tag was found
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
    private static boolean needsToBeRenamed(File sourceFile, String... metadata){
        //Compare current file name against new title
        String currentFileName = sourceFile.getName();
        //Remove extension and get only name
        String currentFileNameWithoutExt = currentFileName.substring(0, currentFileName.length() - 4);
        return !currentFileNameWithoutExt.equals(metadata[0]);
    }

    /**
     * This method check the integrity of file
     *
     * @param absolutePath
     * @return false in case it cannot be read
     */

    private static boolean checkFileIntegrity(String absolutePath){
        File file = new File(absolutePath);
        return file.exists() && file.length() > 0 && file.canRead() && file.canWrite();
    }

    private static boolean checkFileIntegrity(File file){
        return file.exists() && file.length() > 0 && file.canRead() && file.canWrite();
    }

    private static boolean checkFileIntegrity(DocumentFile file){
        return file.exists() && file.length() > 0 && file.canRead() && file.canWrite();
    }

    /**
     * Sets the result code
     * @param resultCode
     */
    private void setMessageCode(int resultCode){
        mError = resultCode;
    }

    /**
     * Gets the result code
     * @return Result code of operation
     */
    public int getMessageCode(){
        return mError;
    }

    /**
     * Compares if any of new tags are the same
     * than current
     */
    private boolean isNeedUpdateTags(){

        //Iterates over new values tag passed, to compare
        //against the values of current tag.
        for(Map.Entry entry : mNewTags.entrySet()){
            //For case of field cover, we need to compare the length of byte array
            if(entry.getKey() == FieldKey.COVER_ART){
                if( (mCurrentArtwork == null) || mCurrentArtwork.length == 0 || mCurrentArtwork.length != ((byte[])entry.getValue()).length){
                    mFieldsToUpdate.put((FieldKey) entry.getKey(), entry.getValue());
                }
            }
            //Compare for other fields if current value tag exist, and if current value tags is different that new
            else {
                if ( ( mCurrentTag.getFirst((FieldKey) entry.getKey()) == null ) ||
                        mCurrentTag.getFirst((FieldKey) entry.getKey()).isEmpty() ||
                        ( !mCurrentTag.getFirst((FieldKey) entry.getKey()).equals(entry.getValue()))
                        ){
                    mFieldsToUpdate.put((FieldKey) entry.getKey(), entry.getValue());
                }
            }
        }

        return mFieldsToUpdate.size() > 0;
    }

    /**
     * Exposes single method to apply new cover
     * or remove existent one
     * @param cover new cover to apply, pass null
     *              to remove current cover.
     * @return true if successful, false otherwise
     */
    public boolean applyCover(@Nullable byte[] cover){
        return internalApplyCover(cover);
    }

    /**
     * Encapsulates the method to apply new cover,
     * taking care if file is store in SD or internal storage.
     * @param cover new cover to apply, pass null
     *              to remove current cover.
     * @return true if successful, false otherwise
     */
    private boolean internalApplyCover(byte[] cover){
        boolean isStoredInSd = StorageHelper.getInstance(sContext).isStoredInSD(mSourceFile);

        if(mCurrentArtwork != null && cover != null && (cover.length == mCurrentArtwork.length)){
            setMessageCode(APPLIED_SAME_COVER);
            return true;
        }


        if(isStoredInSd && Constants.URI_SD_CARD == null){
            setMessageCode(COULD_NOT_GET_URI_SD_ROOT_TREE);
            return false;
        }

        if(isStoredInSd) {
            //File is stored in SD card
            return applyCoverForDocumentFileObject(cover);

        }
        else {
            //File is stored in internal memory
            return applyCoverForFileObject(cover);
        }

    }

    /**
     * Apply coverart for file stored in non removable storage
     * (better known as internal storage).
     * @param cover New cover as byte array, if null is passed, then
     *              delete current cover.
     * @return true if was succesful applied, false otherwise;
     */
    private boolean applyCoverForFileObject(byte[] cover){

        if(cover == null){
            try {
                mCurrentTag.deleteArtworkField();
                mAudioTaggerFile.commit();
                setMessageCode(CURRENT_COVER_REMOVED);
                return true;
            } catch (CannotWriteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                setMessageCode(COULD_NOT_APPLY_COVER);
                return false;
            }
        }
        else {
            Artwork artwork = new AndroidArtwork();
            artwork.setBinaryData(cover);
            try {
                mCurrentTag.deleteArtworkField();
                mCurrentTag.setField(artwork);
                mAudioTaggerFile.commit();
                setMessageCode(NEW_COVER_APPLIED);
                return true;
            } catch (CannotWriteException | FieldDataInvalidException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                setMessageCode(COULD_NOT_REMOVE_COVER);
                return false;
            }
        }
    }

    /**
     * Apply cover art for file stored in SD card
     * @param cover New cover as byte array, if null is passed, then
     *              delete current cover.
     * @return true if was succesful applied, false otherwise;
     */
    private boolean applyCoverForDocumentFileObject(byte[] cover){
        //Tries to creates a temp file in non removable storage to work with it
        File tempFile = StorageHelper.getInstance(sContext).createTempFileFrom(mSourceFile);

        if(tempFile == null) {
            setMessageCode(COULD_NOT_CREATE_TEMP_FILE);
            return false;
        }

        //Creates a audio file from temp file created in which
        //we can apply new data
        AudioFile audioFile = getAudioTaggerFile(tempFile);

        if(audioFile == null) {
            setMessageCode(COULD_NOT_CREATE_AUDIOFILE);
            return false;
        }

        Tag tag = internalGetTag(audioFile);

        if(tag == null){
            setMessageCode(COULD_NOT_READ_TAGS);
            return false;
        }

        if(cover == null){
            try {
                //Delete current cover art and apply the new one
                tag.deleteArtworkField();
                audioFile.commit();
                setMessageCode(CURRENT_COVER_REMOVED);
            } catch (CannotWriteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                setMessageCode(COULD_NOT_APPLY_COVER);
                return false;
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
                setMessageCode(NEW_COVER_APPLIED);
            } catch (FieldDataInvalidException  |CannotWriteException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                setMessageCode(COULD_NOT_REMOVE_COVER);
                return false;
            }
        }

        //Try to copy file to its original SD location
        boolean success = copyBack(tempFile);

        if(!success){
            setMessageCode(COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION);
        }

        deleteTempFile(tempFile);

        return success;
    }


    /**
     * Gets the tag for audio file passed as parameter
     * @param audioFile The audio file to get tag
     * @return The tag fo audio file
     */
    private Tag internalGetTag(AudioFile audioFile){
        Tag tag = null;
        if(mIsMp3){
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
        }

        return audioTaggerFile;
    }

    /**
     *  Gets the document file object corresponding to
     *  SD card
     * @return SD card document file
     */
    private DocumentFile getUriTree(){
        if(Constants.URI_SD_CARD == null) {
            setMessageCode(COULD_NOT_GET_URI_SD_ROOT_TREE);
            return null;
        }
        return DocumentFile.fromTreeUri(sContext, Constants.URI_SD_CARD);
    }

    /**
     * Gets document file corresponding to source file
     * @return DocumentFIle of source file stored in SD location
     * @param sourceFile The file to generate its DocumentFile object
     */
    private DocumentFile internalGetDocumentFile(File sourceFile){
        DocumentFile sourceDocumentFile =  getUriTree();
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
     * Copy temp file to its original location in SD card
     * @return true if was correctly copied the file to its original
     *                  location, false otherwise
     */
    private boolean copyBack(File outputFile) {
        DocumentFile sourceDocumentFile = internalGetDocumentFile(mSourceFile);
        if(sourceDocumentFile == null)
            return false;
        //Check if current file already exist and delete it
        boolean exist = sourceDocumentFile.exists();
        boolean success = false;
        if(exist){
            boolean deleted = sourceDocumentFile.delete();
            Log.d("borrado actual",deleted+"");
        }

        InputStream in = null;
        OutputStream out = null;

        //Copy file with new tags to its original location
        try {
            //First create a DocumentFile object referencing to its original path that we store
            DocumentFile newFile =  sourceDocumentFile.getParentFile().createFile(AudioItem.getMimeType(mSourceFile), mSourceFile.getName() );
            Log.d("newFile uri",newFile.getUri().toString());
            out = sContext.getContentResolver().openOutputStream(newFile.getUri());
            in = new FileInputStream(outputFile);

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            // write the output file
            out.flush();
            out.close();
            success = true;

        } catch (Exception e) {
            e.getMessage();
            success = false;
        }

        return success;
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
     * Deletes file passed in paramete if exist.
     * @param file
     */
    private void deleteTempFile(File file){
        if(file != null && file.exists()){
            file.delete();
        }
    }

    public void releaseResources(){
        mSourceFile = null;
        mError = NOT_SET;
        mAudioTaggerFile = null;
        mAudioHeader = null;
        mIsMp3 = false;
        mCurrentTag = null;
        mCurrentArtwork = null;
        mNewTags = null;
        mFieldsToUpdate = null;
        sTaggerHelper = null;
        sContext = null;
        System.gc();
    }

    public String getMessage(){
        switch (getMessageCode()){
            case TaggerHelper.COULD_NOT_APPLY_COVER:
                return sContext.getString(R.string.message_could_not_apply_cover);
            case TaggerHelper.COULD_NOT_APPLY_TAGS:
                return sContext.getString(R.string.message_could_not_apply_tags);
            case TaggerHelper.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
                return sContext.getString(R.string.message_could_copy_back);
            case TaggerHelper.COULD_NOT_CREATE_AUDIOFILE:
                return sContext.getString(R.string.message_could_not_create_audio_file);
            case TaggerHelper.COULD_NOT_CREATE_TEMP_FILE:
                return sContext.getString(R.string.message_could_not_create_temp_file);
            case TaggerHelper.COULD_NOT_READ_TAGS:
                return sContext.getString(R.string.message_could_not_read_tags);
            case TaggerHelper.COULD_NOT_REMOVE_COVER:
                return sContext.getString(R.string.message_could_not_remove_cover);
            case TaggerHelper.COULD_NOT_GET_URI_SD_ROOT_TREE:
                return sContext.getString(R.string.message_uri_tree_not_set);
            default:
                return sContext.getString(R.string.message_unknown_error);
        }
    }
}
