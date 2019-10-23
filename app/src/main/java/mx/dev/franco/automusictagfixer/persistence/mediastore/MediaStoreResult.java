package mx.dev.franco.automusictagfixer.persistence.mediastore;

import org.jaudiotagger.tag.FieldKey;

import java.util.Map;

public class MediaStoreResult {
    public static final int UPDATE_RENAMED_FILE = 0;
    public static final int UPDATE_TAGS = 1;

    private int task;
    private String newPath;
    private boolean updated;
    private Map<FieldKey, Object> tags;

    public MediaStoreResult() {}

    public MediaStoreResult(boolean updated) {
        this();
        this.updated = updated;
    }

    public MediaStoreResult(String newPath) {
        this();
        this.newPath = newPath;
    }

    public MediaStoreResult(int task) {
        this();
        this.task = task;
    }

    public MediaStoreResult(int task, String newPath, boolean updated) {
        this();
        this.task = task;
        this.newPath = newPath;
        this.updated = updated;
    }

    public int getTask() {
        return task;
    }

    public void setTask(int task) {
        this.task = task;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public Map<FieldKey, Object> getTags() {
        return tags;
    }

    public void setTags(Map<FieldKey, Object> tags) {
        this.tags = tags;
    }
}
