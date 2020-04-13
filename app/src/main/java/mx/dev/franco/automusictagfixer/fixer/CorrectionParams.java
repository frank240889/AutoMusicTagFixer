package mx.dev.franco.automusictagfixer.fixer;

import org.jaudiotagger.tag.FieldKey;

import java.util.Map;

public class CorrectionParams {
    private int correctionMode;
    private int tagsSource;
    private boolean renameFile;
    private String newName;
    private String coverId;
    private String trackId;
    private String mediaStoreId;
    private Map<FieldKey, Object> tags;
    private String target;

    public CorrectionParams(){}

    public int getCorrectionMode() {
        return correctionMode;
    }

    public void setCorrectionMode(int correctionMode) {
        this.correctionMode = correctionMode;
    }

    public int getTagsSource() {
        return tagsSource;
    }

    public void setTagsSource(int tagsSource) {
        this.tagsSource = tagsSource;
    }

    public boolean renameFile() {
        return renameFile;
    }

    public void setRenameFile(boolean renameFile) {
        this.renameFile = renameFile;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getCoverId() {
        return coverId;
    }

    public void setCoverId(String coverId) {
        this.coverId = coverId;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getMediaStoreId() {
        return mediaStoreId;
    }

    public void setMediaStoreId(String mediaStoreId) {
        this.mediaStoreId = mediaStoreId;
    }

    public Map<FieldKey, Object> getTags() {
        return tags;
    }

    public void setTags(Map<FieldKey, Object> tags) {
        this.tags = tags;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
