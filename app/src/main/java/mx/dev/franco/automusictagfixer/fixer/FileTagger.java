package mx.dev.franco.automusictagfixer.fixer;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.interfaces.FileManager;
import mx.dev.franco.automusictagfixer.modelsUI.track_detail.TrackDataLoader;
import mx.dev.franco.automusictagfixer.utilities.Tagger;

/**
 * Created by Franco on 03/09/2019
 */
public class FileTagger implements FileManager<FileTagger.InputParams, TrackDataLoader.TrackDataItem, Tagger.ResultCorrection> {
    private Tagger tagger;

    @Inject
    public FileTagger(Tagger tagger){
        this.tagger = tagger;
    }

    @Override
    public TrackDataLoader.TrackDataItem readFile(String path) {
        try {
            return tagger.readFile(path);
        } catch (ReadOnlyFileException | CannotReadException |
                TagException | InvalidAudioFrameException | IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public Tagger.ResultCorrection writeFile(InputParams input) {
        
    }

    @Override
    public String renameFile(String originalFile, String newName) {

    }


    public static class InputParams {

    }
}
