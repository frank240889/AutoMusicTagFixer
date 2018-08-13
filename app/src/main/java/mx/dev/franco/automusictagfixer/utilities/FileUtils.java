package mx.dev.franco.automusictagfixer.utilities;

import java.io.File;

public class FileUtils {
    public static boolean isAccessible(String path){
        File file = new File(path);
        return file.exists() && file.isFile() && file.canRead() && file.canWrite() && file.length() > 0;
    }
}
