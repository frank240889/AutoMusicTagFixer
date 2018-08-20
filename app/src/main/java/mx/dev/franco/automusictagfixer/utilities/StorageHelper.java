package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseArray;

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
    private SparseArray<String> mBasePaths = new SparseArray<>();
    private StorageHelper(Context context){
        sContext = context.getApplicationContext();
    }

    public static StorageHelper getInstance(Context context){
        if(sStorage == null) {
            sStorage = new StorageHelper(context);
        }

        return sStorage;
    }

    public int getNumberAvailableMediaStorage (){
        return ContextCompat.getExternalFilesDirs(sContext, null).length;
    }

    public boolean isPresentRemovableStorage(){
        return (mBasePaths.size() > 1);
    }

    /**
     * Detect number of storage available.
     */
    public StorageHelper detectStorages(){
        File[] storage = ContextCompat.getExternalFilesDirs(sContext, "temp_tagged_files");

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

    public SparseArray<String> getBasePaths(){
        return mBasePaths;
    }

    /**
     * Gets current available size
     * @return available size of current storage
     */
    private static long getInternalAvailableSize(){
        return Environment.getExternalStorageDirectory().getTotalSpace();//ContextCompat.getExternalFilesDirs(sContext, "temp_tagged_files")[0].getUsableSpace();
    }

    /**
     * Creates a temo file in external non-removable storage,
     * more known as shared Storage or internal storage
     * @param sourceFile
     * @return
     */
    public File createTempFileFrom(File sourceFile) {

        //Before create temp file, check if exist enough space,
        //to ensure iwe can perform correctly the operations, lets take the triple size of source file
        //because operations of AudioTagger library.
        long availableSize = getInternalAvailableSize();
        long fileSize = sourceFile.getTotalSpace();
        if(availableSize < (fileSize * 3) ) {

            return null;
        }


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

    public boolean isStoredInSD(File file){
        return internalIsStoredInSD(file);
    }

    /**
     * Check if file is stored on SD card or Non removable storage.
     * @return
     */
    private boolean internalIsStoredInSD(File file){
        SparseArray<String> basePaths =  StorageHelper.getInstance(sContext).getBasePaths();
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
