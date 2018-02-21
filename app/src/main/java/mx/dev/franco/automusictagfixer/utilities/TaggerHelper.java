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
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.images.AndroidArtwork;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.list.AudioItem;

import static mx.dev.franco.automusictagfixer.list.AudioItem.getExtension;

/**
 * Created by franco on 16/01/18.
 * This class was created to delegate
 * the responsibility of update tags, because implies
 * additional functionality when files are
 * stored on sd card.
 *
 * Is implemented by using part of Storage Access Framework
 * because is the only way to modify data when is stored
 * in SD.
 */

public final class TaggerHelper {
    private static final String TAG = TaggerHelper.class.getName();
    private static TaggerHelper sTaggerHelper;
    private Tag mInputTag;
    private static Context sContext;
    private File mSourceFile;
    private String mError = "";
    private boolean mOverWriteAllTags = false;


    private TaggerHelper(Context context) {
        sContext = context.getApplicationContext();
    }

    /**
     * Creates a singleton of this class
     * @param context The context object to create this singleton
     * @return TaggerHelper singleton
     */
    public static TaggerHelper getInstance(Context context) {
        if (sTaggerHelper == null)
            sTaggerHelper = new TaggerHelper(context);

        return sTaggerHelper;
    }

    /**
     * Sets the file to which will apply new tags
     * @param file
     * @return TaggerHelper object to chain calls to subsequent methods
     */
    public TaggerHelper setSourceFile(File file){
        mSourceFile = file;
        return sTaggerHelper;
    }

    /**
     * Sets new tags to apply
     * @param tag
     * @return TaggerHelper object to chain calls to subsequent methods
     */
    public TaggerHelper setTags(Tag tag) {
        mInputTag = tag;
        return sTaggerHelper;
    }

    /**
     *  Gets the document file object corresponding to
     *  SD card
     * @return SD card document file
     */
    private DocumentFile getUriTree(){
        if(Constants.URI_SD_CARD == null) {
           setError("No permission has granted");
            return null;
        }
        return DocumentFile.fromTreeUri(sContext, Constants.URI_SD_CARD);
    }

    /**
     * Gets document file correspinding to source file
     * @return DocumentFIle of source file stored in SD location
     */
    private DocumentFile internalGetDocumentFile(){
        DocumentFile sourceDocumentFile =  getUriTree();
        if(sourceDocumentFile == null){
            return null;
        }

        String relativePath  = getRelativePath(mSourceFile);

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

    private boolean internalApplyTags() {
        //Creates a temp file in non removable storage to apply tags
        File tempFile = StorageHelper.withContext(sContext).createTempFileFrom(mSourceFile);
        boolean success = false;

        if(tempFile == null) {
            setError("Could not create temp file.");
            return success;
        }

        AudioFile audioFile = getAudioTaggerFile(tempFile);

        if(audioFile == null) {
            setError("Could not create audio file.");
            return success;
        }
        boolean isMp3 = (AudioItem.getMimeType(mSourceFile).equals("audio/mpeg_mp3") ||
                ( AudioItem.getMimeType(mSourceFile).equals("audio/mpeg")  && AudioItem.getExtension(mSourceFile).toLowerCase().equals("mp3")  ) );

        if(isMp3 && ((MP3File) audioFile).hasID3v1Tag()){
            //remove old version of ID3 tags
            //Log.d("removed ID3v1","remove ID3v1");
            try {
                ((MP3File) audioFile).delete( ((MP3File)audioFile).getID3v1Tag() );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Todo: Make condition to overwrite all tags or only apply those missing;
        //Delete current tags from temp file
        //and apply news
        try {
            AudioFileIO.delete(audioFile);
        } catch (CannotReadException | CannotWriteException  e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        //Sets new tags
        audioFile.setTag(mInputTag);

        //Apply tags
        try {
            audioFile.commit();
        } catch (CannotWriteException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
            setError("Could not apply tags.");
            return false;
        }

        success = copyBack(tempFile);

        if(!success){
            setError("Could not copy back file to its original location");
        }

        deleteTempFile(tempFile);

        mSourceFile = null;
        mInputTag = null;

        return success;
    }

      @Nullable
      public String renameFile(String... metadata) throws NullPointerException {
        DocumentFile currentDocumentFile = internalGetDocumentFile();
        if(currentDocumentFile == null){
        throw new NullPointerException("Could not create document file.");
        }

        if(!AudioItem.checkFileIntegrity(currentDocumentFile)){
            return null;
        }


          String currentParentPath = mSourceFile.getParent();
          Log.d("parent path source file", currentParentPath);
          String newFilename = StringUtilities.sanitizeFilename(metadata[0]) + "." + getExtension(mSourceFile.getName());
          String newRelativeFilePath= currentParentPath + "/" + newFilename;

          String id = DocumentsContract.getDocumentId(getUriTree().getUri());

          //Check if new file document already exist
        id = id + getRelativePath(new File(newRelativeFilePath));
        Uri childUri = DocumentsContract.buildDocumentUriUsingTree(Constants.URI_SD_CARD, id);
        DocumentFile renamedDocument = DocumentFile.fromSingleUri(sContext, childUri);
        boolean wasRenamed = false;
        if(renamedDocument != null){
            if(renamedDocument.exists()){
                newFilename = StringUtilities.sanitizeFilename(metadata[0]) + "-"+metadata[1] + "." + getExtension(mSourceFile.getName());
            }
            else {
                newFilename = StringUtilities.sanitizeFilename(metadata[0]) + "." + getExtension(mSourceFile.getName());
            }
            newRelativeFilePath= currentParentPath + "/" + newFilename;
            wasRenamed = currentDocumentFile.renameTo(newFilename);
        }

        if(wasRenamed)
            return newRelativeFilePath;

        return null;

    }

    private void setError(String msgError){
        mError = msgError;
    }

    public String getError(){
        return mError;
    }

    public boolean applyTags() {
        return internalApplyTags();
    }

    public boolean applyCover(byte[] cover){
        return internalApplyCover(cover);
    }

    private boolean internalApplyCover(byte[] cover){
        //Creates a temp file in non removable storage to apply tags
        File tempFile = StorageHelper.withContext(sContext).createTempFileFrom(mSourceFile);
        boolean success = false;

        if(tempFile == null) {
            setError("Could not create temp file.");
            return success;
        }

        AudioFile audioFile = getAudioTaggerFile(tempFile);

        if(audioFile == null) {
            setError("Could not create audio file.");
            return success;
        }

        Tag tag = getTag(audioFile);

        try {
            //Delete current cover art and apply the new one
            Artwork artwork = new AndroidArtwork();
            artwork.setBinaryData(cover);
            tag.deleteArtworkField();
            tag.setField(artwork);
            audioFile.commit();
        } catch (FieldDataInvalidException  |CannotWriteException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
            setError("Could not apply cover.");
            return false;
        }

        success = copyBack(tempFile);

        if(!success){
            setError("Could not copy back file to its original location");
        }

        deleteTempFile(tempFile);

        mSourceFile = null;

        return success;
    }

    public boolean removeCover(){
        return internalRemoveCover();
    }

    private boolean internalRemoveCover(){
        //Creates a temp file in non removable storage to apply tags
        File tempFile = StorageHelper.withContext(sContext).createTempFileFrom(mSourceFile);
        boolean success = false;

        if(tempFile == null) {
            setError("Could not create temp file.");
            return success;
        }

        AudioFile audioFile = getAudioTaggerFile(tempFile);
        Tag tag = getTag(audioFile);

        if(audioFile == null) {
            setError("Could not create audio file.");
            return success;
        }


        //Apply chnages to tag in temp file
        try {
            //Delete current cover
            tag.deleteArtworkField();
            audioFile.commit();
        } catch (CannotWriteException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
            setError("Could not remove cover.");
            return false;
        }

        success = copyBack(tempFile);

        if(!success){
            setError("Could not copy back file to its original location");
        }

        deleteTempFile(tempFile);

        mSourceFile = null;

        return success;
    }


    private Tag getTag(AudioFile audioFile){
        Tag tag = null;
        boolean isMp3 = (AudioItem.getMimeType(mSourceFile).equals("audio/mpeg_mp3") ||
                ( AudioItem.getMimeType(mSourceFile).equals("audio/mpeg")  && AudioItem.getExtension(mSourceFile).toLowerCase().equals("mp3")  ) );
        if(isMp3){
            if(((MP3File)audioFile).hasID3v1Tag() && !((MP3File) audioFile).hasID3v2Tag()){
                //create new version of ID3v2
                ID3v24Tag id3v24Tag = new ID3v24Tag( ((MP3File)audioFile).getID3v1Tag() );
                audioFile.setTag(id3v24Tag);
                tag = ((MP3File) audioFile).getID3v2TagAsv24();
                //Log.d("converted_tag","converted_tag");
            }
            else {
                //read existent V2 tag
                if(((MP3File) audioFile).hasID3v2Tag()) {
                    tag = ((MP3File) audioFile).getID3v2Tag();
                    //Log.d("get_v24_tag","get_v24_tag");
                }
                //Has no tags? create a new one, but don't save until
                //user apply changes
                else {
                    ID3v24Tag id3v24Tag = new ID3v24Tag();
                    ((MP3File) audioFile).setID3v2Tag(id3v24Tag);
                    tag = ((MP3File) audioFile).getID3v2Tag();
                    //Log.d("create_v24_tag","create_v24_tag");
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
     * Create a AudioFile to which are going to apply new tags
     * @return Audiofile object from temp file
     */
    private AudioFile  getAudioTaggerFile(File tempFile){
        AudioFile audioTaggerFile = null;
        try {
            audioTaggerFile = AudioFileIO.read(tempFile);
        }
        catch (IOException | CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
            e.printStackTrace();
        }

        return audioTaggerFile;
    }

    public void releaseResources(){
        //Clear references to current file
        //and its tags
        mSourceFile = null;
        mInputTag = null;
        sContext = null;
        System.gc();
    }

    /**
     * Copy temp file to its original location in SD card
     * @return true if was correctly copied the file to its original
     *                  location, false otherwise
     */
    private boolean copyBack(File outputFile) {
        DocumentFile sourceDocumentFile = internalGetDocumentFile();
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
            DocumentFile newFile =  sourceDocumentFile.getParentFile().createFile("audio/mp3", mSourceFile.getName() );
            Log.d("newFile uri",newFile.getUri().toString());
            out = sContext.getContentResolver().openOutputStream(newFile.getUri());
            in = new FileInputStream(outputFile);

            byte[] buffer = new byte[1024];
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
        if (externalFilesDirs != null && externalFilesDirs.length > 0) {
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

    public TaggerHelper overWriteAllTags(boolean overwriteAllTags){
        mOverWriteAllTags = overwriteAllTags;
        return this;
    }
    private void deleteTempFile(File file){
        if(file != null && file.exists()){
            file.delete();
        }
    }
}
