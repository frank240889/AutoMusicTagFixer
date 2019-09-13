package mx.dev.franco.automusictagfixer.fixer;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.interfaces.FileManager;

/**
 * Created by Franco on 03/09/2019
 */
public class FileTagger implements FileManager<FileTagger.InputParams, AudioTagger.TrackDataItem, AudioTagger.ResultCorrection> {
    private AudioTagger tagger;

    @Inject
    public FileTagger(AudioTagger tagger){
        this.tagger = tagger;
    }

    @Nullable
    @Override
    public AudioTagger.TrackDataItem readFile(String path) {
        try {
            return tagger.readFile(path);
        } catch (ReadOnlyFileException | CannotReadException |
                TagException | InvalidAudioFrameException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @Override
    public AudioTagger.ResultCorrection writeFile(InputParams input) {
        switch (input.codeRequest) {
            case AudioTagger.MODE_ADD_COVER:
                try {
                    return tagger.applyCover((byte[]) input.fields.get(FieldKey.COVER_ART), input.path);
                }
                catch (ReadOnlyFileException | IOException | TagException
                        | InvalidAudioFrameException | CannotReadException e) {
                    e.printStackTrace();
                    return null;
                }
            case AudioTagger.MODE_REMOVE_COVER:
                try {
                    return tagger.applyCover(null, input.path);
                }
                catch (ReadOnlyFileException | IOException | TagException
                        | InvalidAudioFrameException | CannotReadException e) {
                    e.printStackTrace();
                    return null;
                }
            case AudioTagger.MODE_WRITE_ONLY_MISSING:
            case AudioTagger.MODE_OVERWRITE_ALL_TAGS:
                try {
                    return tagger.saveTags(input.path, (HashMap<FieldKey, Object>) input.fields, input.codeRequest);
                }
                catch (ReadOnlyFileException | IOException | TagException
                        | InvalidAudioFrameException | CannotReadException e) {
                    e.printStackTrace();
                    return null;
                }
        }
        return null;
    }

    @Nullable
    @Override
    public String renameFile(String originalFile, String newName) {
        return tagger.renameFile(new File(originalFile), newName);
    }


    public static class InputParams {
        private String path;
        private Map<FieldKey, Object> fields;
        private int codeRequest;
    }
}
