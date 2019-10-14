package mx.dev.franco.automusictagfixer.utilities;

import mx.dev.franco.automusictagfixer.common.Action;

public class IdentificationType extends ActionableMessage {
    public static final int ALL_TAGS = 0;
    public static final int ONLY_COVER = 1;


    private int identificationType = ALL_TAGS;

    public IdentificationType() {
        super();
    }

    public IdentificationType(Action action, String message, int identificationType) {
        super(action, message);
        this.identificationType = identificationType;
    }

    public IdentificationType(Action action, String message, String details, int identificationType) {
        super(action, message, details);
        this.identificationType = identificationType;
    }

    public IdentificationType(Action action, int idResourceMessage, int identificationType) {
        super(action, idResourceMessage);
        this.identificationType = identificationType;
    }

    public IdentificationType(Action action, int idResourceMessage, String details, int identificationType) {
        super(action, idResourceMessage, details);
        this.identificationType = identificationType;
    }

    public int getIdentificationType() {
        return identificationType;
    }

    public void setIdentificationType(int identificationType) {
        this.identificationType = identificationType;
    }
}
