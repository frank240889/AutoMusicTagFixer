package mx.dev.franco.automusictagfixer.filemanager;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by franco on 3/10/17.
 */

public final class ImageFileSaver {
    private static final String SLASH = "/";
    private static final String AUTO_MUSIC_TAG_FIXER_FOLDER_NAME = "Covers";
    private static final String EXTENSION = "jpg";
    private static final String DOT = ".";
    public static final String NULL_DATA = "null data";
    public static final String NO_EXTERNAL_STORAGE_WRITABLE = "no external storage writable";
    public static final String INPUT_OUTPUT_ERROR = "i/o onIdentificationError";
    public static final String GENERIC_NAME = "Unknown_cover";

    /**
     *
     * @param data Image data
     * @param imageName The title of song if exist
     * @param artist The artist of song if exist
     * @param album The album of song if exist
     * @return string absolute path where image was saved or
     *                  any other string representing the onIdentificationError.
     * @throws IOException
     */
    public static String saveImageFile(@Nonnull byte[] data, @Nullable String imageName) throws IOException {

        //No data to write
        if(data == null)
            return NULL_DATA;

        //External storage es not writable
        if(!isExternalStorageWritable()){
            return NO_EXTERNAL_STORAGE_WRITABLE;
        }


        //Retrieve folder app, and if doesn't exist create it before
        File pathToFile = getAlbumStorageDir();

        if(imageName == null || imageName.isEmpty())
            imageName = GENERIC_NAME;

        //File object representing the new image file
        File imageFileCreated = createFile(pathToFile, imageName);

        //Stream to write
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFileCreated);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            if(fos != null){
                fos.close();
            }
            return INPUT_OUTPUT_ERROR;
        }
        return imageFileCreated.getAbsolutePath();

    }

    /**
     * Generates filename, appending the
     * current data and time to avoid repeat
     * file names
     * @param pathToFile Absolute path where will be saved the image
     * @param imageFile The title of song if exist
     * @param artist The artist of song if exist
     * @param album The album of song if exist
     * @return File representing the image
     */
    private static File createFile(File pathToFile, String imageFile) {
        //Get and format date
        Date date = new Date();
        DateFormat now = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String newFileName = imageFile + "_" + now.format(date);

        return new File(pathToFile.getAbsolutePath() + SLASH + newFileName + DOT + EXTENSION);
    }

    /**
     * Checks if external storage is available for read and write
     * */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     *Checks if external storage is available for reading at least
     */
    private static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    /**
     *  Get the directory for the user's public pictures directory.
     * @return File representing the absolute path where images
     *                  are going to be saved
     */
    private static File getAlbumStorageDir() {
        return createFolderIfNotExist();
    }


    private static File createFolderIfNotExist() {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), AUTO_MUSIC_TAG_FIXER_FOLDER_NAME);
        if(!file.exists())
            file.mkdirs();

        return file;
    }
}
