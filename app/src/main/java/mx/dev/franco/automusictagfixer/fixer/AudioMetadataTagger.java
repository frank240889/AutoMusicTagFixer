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

import mx.dev.franco.automusictagfixer.interfaces.AudioMetadataManager;

/**
 * Created by Franco on 03/09/2019
 */
public class AudioMetadataTagger implements AudioMetadataManager<AudioMetadataTagger.InputParams,
        AudioTagger.AudioFields, AudioTagger.ResultCorrection> {

    private AudioTagger tagger;

    @Inject
    public AudioMetadataTagger(AudioTagger tagger){
        this.tagger = tagger;
    }

    @Nullable
    @Override
    public AudioTagger.AudioFields readMetadata(String path)
            throws IOException, TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException {

        return tagger.readFile(path);
    }

    @Nullable
    @Override
    public AudioTagger.ResultCorrection writeMetadata(InputParams input)
            throws ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException, IOException {

        switch (input.codeRequest) {
            case AudioTagger.MODE_ADD_COVER:
                return tagger.applyCover((byte[]) input.fields.get(FieldKey.COVER_ART), input.path);
            case AudioTagger.MODE_REMOVE_COVER:
                return tagger.applyCover(null, input.path);
            case AudioTagger.MODE_WRITE_ONLY_MISSING:
            case AudioTagger.MODE_OVERWRITE_ALL_TAGS:
                return tagger.saveTags(input.path, (HashMap<FieldKey, Object>) input.fields, input.codeRequest);
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
