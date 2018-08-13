package mx.dev.franco.automusictagfixer.utilities.FileReaderImpl;

import mx.dev.franco.automusictagfixer.interfaces.ItemHandler;
import mx.dev.franco.automusictagfixer.list.AudioItem;

public class FileReader implements ItemHandler<AudioItem>, AsyncFileReader.FileReadingOperationListener {
    private AsyncFileReader mAsyncFileReader;
    public FileReader(){

    }

    @Override
    public void read(long id) {

    }

    @Override
    public void readAll() {

    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onProgressUpdate(AudioItem audioItem) {

    }

    @Override
    public void onPostExecute() {

    }

    @Override
    public void onCancelled() {

    }
}
