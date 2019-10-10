package mx.dev.franco.automusictagfixer.fixer;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import mx.dev.franco.automusictagfixer.interfaces.AudioMetadataManager;

/**
 * Created by Franco on 03/09/2019
 */
public class AudioMetadataTagger implements AudioMetadataManager<AudioMetadataTagger.InputParams,
        AudioTagger.AudioFields, AudioTagger.ResultCorrection> {

    private AudioTagger tagger;

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
                return tagger.applyCover((byte[]) input.fields.get(FieldKey.COVER_ART), input.targetFile);
            case AudioTagger.MODE_REMOVE_COVER:
                return tagger.applyCover(null, input.targetFile);
            case AudioTagger.MODE_WRITE_ONLY_MISSING:
            case AudioTagger.MODE_OVERWRITE_ALL_TAGS:
                return tagger.saveTags(input.targetFile, input.fields, input.codeRequest);
        }
        return null;
    }

    @Nullable
    @Override
    public String renameFile(String absolutePathTargetFile, String newName) {
        return tagger.renameFile(new File(absolutePathTargetFile), newName);
    }


    /**
     * Class to wrap required data for this tagger to correct the metadata.
     */
    public static class InputParams {
        private String targetFile;
        private Map<FieldKey, Object> fields;
        private int codeRequest;

        public InputParams() {
        }

        public InputParams(String targetFile) {
            this.targetFile = targetFile;
        }

        public InputParams(Map<FieldKey, Object> fields) {
            this.fields = fields;
        }

        public InputParams(String targetFile, Map<FieldKey, Object> fields) {
            this.targetFile = targetFile;
            this.fields = fields;
        }

        public InputParams(String targetFile, Map<FieldKey, Object> fields, int codeRequest) {
            this.targetFile = targetFile;
            this.fields = fields;
            this.codeRequest = codeRequest;
        }

        public String getTargetFile() {
            return targetFile;
        }

        public void setTargetFile(String targetFile) {
            this.targetFile = targetFile;
        }

        public Map<FieldKey, Object> getFields() {
            return fields;
        }

        public void setFields(Map<FieldKey, Object> fields) {
            this.fields = fields;
        }

        public int getCodeRequest() {
            return codeRequest;
        }

        public void setCodeRequest(int codeRequest) {
            this.codeRequest = codeRequest;
        }
    }
}
