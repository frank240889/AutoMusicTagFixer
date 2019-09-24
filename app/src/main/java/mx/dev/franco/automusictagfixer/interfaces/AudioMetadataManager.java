package mx.dev.franco.automusictagfixer.interfaces;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

import javax.annotation.Nullable;
/**
 * Created by Franco on 03/09/2019
 */
public interface AudioMetadataManager<I,O,R> {
    @Nullable
    O readMetadata(String path)
            throws IOException, TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException;
    @Nullable
    R writeMetadata(I input)
            throws IOException, ReadOnlyFileException, CannotReadException, TagException, InvalidAudioFrameException;
    @Nullable
    String renameFile(String absolutePathTargetFile, String newName);
}
