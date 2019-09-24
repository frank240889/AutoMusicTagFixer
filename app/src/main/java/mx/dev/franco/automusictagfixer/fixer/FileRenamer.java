package mx.dev.franco.automusictagfixer.fixer;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class FileRenamer extends AbstractMetadataFixer<Void, Void, AudioTagger.ResultRename> {
    private AsyncOperation<Track, MetadataWriterResult, Track, MetadataWriterResult> mCallback;
    private String mNewName;
    public FileRenamer(AsyncOperation<Track, MetadataWriterResult, Track, MetadataWriterResult> callback,
                       AudioMetadataTagger fileTagger,
                       Track track, String newName) {
        super(fileTagger, track);
        mCallback = callback;
        mNewName = newName;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(track);
    }

    @Override
    protected AudioTagger.ResultRename doInBackground(Void... voids) {

        AudioTagger.ResultRename resultRename = new AudioTagger.ResultRename();

        String newName = mFileTagger.renameFile(track.getPath(), mNewName);

        if(newName != null)
            resultRename.setNewAbsolutePath(newName);
        else
            resultRename.setCode(AudioTagger.COULD_NOT_RENAME_FILE);

        return resultRename;
    }

    @Override
    protected void onPostExecute(AudioTagger.ResultRename resultCorrection) {
        if(mCallback != null) {
            MetadataWriterResult result = new MetadataWriterResult(track, resultCorrection);
            if(resultCorrection.getCode() != AudioTagger.SUCCESS) {
                mCallback.onAsyncOperationError(result);
            }
            else {
                mCallback.onAsyncOperationFinished(result);
            }
        }
    }

    @Override
    protected void onCancelled(AudioTagger.ResultRename resultCorrection) {
        onCancelled();
        if(mCallback != null)
            mCallback.onAsyncOperationCancelled(track);
    }
}
