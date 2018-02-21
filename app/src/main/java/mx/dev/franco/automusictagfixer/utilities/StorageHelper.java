package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by franco on 12/01/18.
 */

public class StorageHelper{
    private static Context sContext;
    private static StorageHelper sStorage;
    private StorageHelper(){
    }

    public static StorageHelper withContext(Context context){
        if(sContext == null)
            sContext = context;
        if(sStorage == null)
            sStorage = new StorageHelper();

        return sStorage;
    }

    public int getNumberAvailableMediaStorage (){
        return ContextCompat.getExternalFilesDirs(sContext, null).length;
    }

    public boolean isPresentRemovableStorage(){
        File[] storage = ContextCompat.getExternalFilesDirs(sContext, "temp_tagged_files");

        //if number of media storage is 1, means that only
        //external no removable (internal device storage) is
        //available
        if(storage.length <= 1)
            return false;

        int numberMountedStorage = 0;

        for(File s : storage){
            //When SD card is removed sometimes storage hold a reference to this
            //folder, so if the reference is null, means the storage has unmounted or removed
            //and is not available anymore
            if(s != null) {
                numberMountedStorage++;
                Log.d("storage", s.toURI().toString() + "");
            }
        }

        return (numberMountedStorage > 1);
    }


    public long getAvailableSize(){
        return getInternalAvailableSize();
    }

    /**
     * Gets current available size
     * @return available size of current storage
     */
    private static long getInternalAvailableSize(){
        return sContext.getExternalFilesDir(null).getFreeSpace();
    }

    /**
     * Creates a temo file in external no-removable storage,
     * more known as shared Storage or internal storage
     * @param sourceFile
     * @return
     */
    public File createTempFileFrom(File sourceFile) {

        //Before create temp file, check if exist enough space,
        //to ensure is enough, lets take the double of tempFile size
        if(getInternalAvailableSize() < (sourceFile.getTotalSpace() * 2) )
            return null;


        // Create a path where we will place our private file on non removable external
        // storage.
        File externalNonRemovableDevicePath = ContextCompat.getExternalFilesDirs(sContext, "temp_tagged_files")[0];

        File fileDest = new File(externalNonRemovableDevicePath, sourceFile.getName());

        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = new FileInputStream(sourceFile).getChannel();
            outChannel = new FileOutputStream(fileDest).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
                }
            if (outChannel != null)
                try {
                    outChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return fileDest;
    }
}
