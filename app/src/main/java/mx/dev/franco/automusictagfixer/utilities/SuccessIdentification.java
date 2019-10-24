package mx.dev.franco.automusictagfixer.utilities;

public class SuccessIdentification {
    public static final int ALL_TAGS = 0;
    public static final int ONLY_COVER = 1;
    private int identificationType = ALL_TAGS;
    private String mediaStoreId;

    public SuccessIdentification() {}

    public SuccessIdentification(int identificationType, String mediaStoreId) {
        this();
        this.identificationType = identificationType;
        this.mediaStoreId = mediaStoreId;
    }

    public int getIdentificationType() {
        return identificationType;
    }

    public void setIdentificationType(int identificationType) {
        this.identificationType = identificationType;
    }

    public String getMediaStoreId() {
        return mediaStoreId;
    }

    public void setMediaStoreId(String mediaStoreId) {
        this.mediaStoreId = mediaStoreId;
    }
}
