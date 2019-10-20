package mx.dev.franco.automusictagfixer.identifier;

public class IdentificationParams {
    public static int ALL_TAGS = 0;
    public static int ONLY_COVER = 1;

    private int identificationType;

    public IdentificationParams() {
    }

    public IdentificationParams(int identificationType) {
        this();
        this.identificationType = identificationType;
    }

    public int getIdentificationType() {
        return identificationType;
    }

    public void setIdentificationType(int identificationType) {
        this.identificationType = identificationType;
    }
}
