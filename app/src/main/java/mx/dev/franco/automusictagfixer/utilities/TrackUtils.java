package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.webkit.MimeTypeMap;

import java.io.File;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

/**
 * Created by franco on 14/04/17.
 */

public final class TrackUtils {
    public static final float KILOBYTE = 1048576;
    /**
     * Formats duration to human readable
     * string
     * @param duration duration in seconds
     * @return formatted string duration
     */
    public static String getHumanReadableDuration(String duration){
        if(duration == null || duration.isEmpty())
            return "0";
        int d = Integer.parseInt(duration);
        int minutes = 0;
        int seconds = 0;
        String readableDuration = "\'" + "00" +  "\"" + "00";
        minutes = (int) Math.floor(d / 60);
        seconds = d % 60;
        readableDuration = minutes + "\'" + (seconds<10?("0"+seconds):seconds) + "\"";
        return readableDuration;
    }

    /**
     * Gets file size in megabytes
     * @param size File size
     * @return formatted file size string
     */
    public static String getFileSize(long size){
        if(size <= 0)
            return "0 Mb";

        float s = size / KILOBYTE;
        String str = String.valueOf(s);
        int l = str.length();
        String readableSize = "";
        if(l > 4)
            readableSize = str.substring(0,4);
        else
            readableSize =str.substring(0,3);
        readableSize += " mb";

        return readableSize;
    }

    public static String getFileSize(String path){
        File file = new File(path);
        if(!Tagger.checkFileIntegrity(file))
            return "0 Mb";

        return getFileSize(file.length());
    }

    /**
     * Gets image dimensions information
     * @param cover Cover art data
     * @return formatted string image size
     */
    public static String getStringImageSize(byte[] cover, Context context){
        String msg = context.getString(R.string.missing_cover);
        if(cover != null && cover.length > 0) {
            try {
                Bitmap bitmapDrawable = BitmapFactory.decodeByteArray(cover, 0, cover.length);
                msg = bitmapDrawable.getHeight() + " * " + bitmapDrawable.getWidth() +" " +
                        context.getString(R.string.pixels);
            }
            catch (Exception e){
                msg = context.getString(R.string.missing_cover);
            }
        }
        return msg;
    }

    /**
     * Gets image dimensions information
     * @param cover Cover art data
     * @return formatted string image size
     */
    public static String getStringImageSize(byte[] cover, ResourceManager resourceManager){
        String msg = resourceManager.getString(R.string.missing_cover);
        if(cover != null && cover.length > 0) {
            try {
                Bitmap bitmapDrawable = BitmapFactory.decodeByteArray(cover, 0, cover.length);
                msg = bitmapDrawable.getHeight() + " * " + bitmapDrawable.getWidth() + " " +
                        resourceManager.getString(R.string.pixels);
            }
            catch (Exception e){
                msg = resourceManager.getString(R.string.missing_cover);
            }
        }
        return msg;
    }


    /**
     * Returns only the last part of absolute path,
     * it means, the file name
     * @param absolutePath
     * @return
     */
    public static String getFilename(String absolutePath){
        if(absolutePath == null || absolutePath.isEmpty())
            return "";

        String[] parts = absolutePath.split("/");
        return parts[parts.length-1];
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

    public static String getMimeType(String absolutePath){
        String ext = getExtension(absolutePath);
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

    public static String getExtension(String absolutePath){
        if(absolutePath == null || absolutePath.isEmpty())
            return null;

        File file = new File(absolutePath);
        //get file extension, extension must be in lowercase
        return file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
    }

    public static String getExtension(File file){
        if(file == null)
            return null;
        //get file extension, extension must be in lowercase
        return file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
    }

    public static String getPath(String path){
        if(path == null)
            return null;
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static String getPath(File file){
        if(file == null)
            return null;
        return getPath(file.getAbsolutePath());
    }
}
