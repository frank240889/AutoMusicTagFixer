package mx.dev.franco.automusictagfixer.interfaces;

/**
 * Created by Franco on 03/09/2019
 */
public interface FileManager<I,O,R> {
    O readFile(String path);
    R writeFile(I input);
    String renameFile(String originalFile, String newName);
}
