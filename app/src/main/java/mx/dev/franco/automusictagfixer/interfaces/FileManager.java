package mx.dev.franco.automusictagfixer.interfaces;

import javax.annotation.Nullable;
/**
 * Created by Franco on 03/09/2019
 */
public interface FileManager<I,O,R> {
    @Nullable
    O readFile(String path);
    @Nullable
    R writeFile(I input);
    @Nullable
    String renameFile(String originalFile, String newName);
}
