package mx.dev.franco.musicallibraryorganizer.utilities;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Created by franco on 3/10/17.
 */

public final class FileSaver {
    public static final String SLASH = "/";
    public static final String AUTO_MUSIC_TAG_FIXER_FOLDER_NAME = "Covers";
    public static final String EXTENSION = "jpg";
    public static final String DOT = ".";
    private static final String GENERIC_NAME = "cover";
    public static final String NULL_DATA = "null data";
    public static final String NO_EXTERNAL_STORAGE_WRITABLE = "no external storage writable";
    public static final String INPUT_OUTPUT_ERROR = "i/o error";


    public static String saveFile(byte[] data, String fileName, String artist, String album){

        //No data to write
        if(data == null)
            return NULL_DATA;

        //External storage es not writable
        if(!isExternalStorageWritable()){
            Log.e("error", "no external storage writable");
            return NO_EXTERNAL_STORAGE_WRITABLE;
        }


        //Retrieve folder app, and if doesn't exist create it before
        File pathToFile = getAlbumStorageDir(AUTO_MUSIC_TAG_FIXER_FOLDER_NAME);

        //File object representing the new image file
        File fileGenerated = generateFileName(pathToFile, fileName, artist, album);

        //Stream to write
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileGenerated);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return INPUT_OUTPUT_ERROR;
        }
        return fileGenerated.getAbsolutePath();

    }

    /**
     * Generates filename, checking if file
     * already exist, if yes, then call itself again
     * until a filename non existing be created
     * @param pathToFile
     * @param fileName
     * @param artist
     * @param album
     * @return
     */
    private static File generateFileName(File pathToFile, String fileName, String artist, String album){
        File newImage = null;
        String name = "";
        if(!fileName.equals("")) {
            newImage = new File(pathToFile.getAbsolutePath() + SLASH + fileName + DOT + EXTENSION);
            name = fileName;
        }
        else if(!artist.equals("")) {
            newImage = new File(pathToFile.getAbsolutePath() + SLASH + artist + DOT + EXTENSION);
            name = artist;
        }
        else if(!album.equals("")){
            newImage = new File(pathToFile.getAbsolutePath() + SLASH + artist + DOT + EXTENSION);
            name = album;
        }
        else {
            Random random = new Random();
            String completeName = GENERIC_NAME + random.nextInt(100) + "_" + random.nextInt(100);
            newImage = new File(pathToFile.getAbsolutePath() + SLASH + completeName + DOT + EXTENSION);
            name = completeName;
        }

        if(!newImage.exists()){
            return newImage;
        }
        fileName += GENERIC_NAME + name;
        return generateFileName(pathToFile, fileName, artist, album);
    }

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        Log.e("error","not mounted");
        return false;
    }

    /* Checks if external storage is available to at least read */
    private static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private static File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.d("info","folder already created");
        }
        return file;
    }
}
